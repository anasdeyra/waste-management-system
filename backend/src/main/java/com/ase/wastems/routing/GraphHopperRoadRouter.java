package com.ase.wastems.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.ev.Subnetwork;
import com.graphhopper.routing.util.DefaultSnapFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.Constants;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;
import com.graphhopper.util.TurnCostsConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Embedded GraphHopper on the versioned OSM snapshot (D-021, D-037). The graph is built lazily
 * on first use and cached on disk per version. Failures are not cached: every call retries and
 * fails again with the same explicit message. GraphHopper takes (latitude, longitude) while
 * this project's {@link WgsPoint} is (longitude, latitude).
 */
@Component
public class GraphHopperRoadRouter implements RoadRouter, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(GraphHopperRoadRouter.class);

    public static final String ENGINE = "GraphHopper " + Constants.VERSION
            + " (profile car: custom model car.json, CH, OSM turn restrictions)";

    private final RoutingProperties props;
    private final ResourceLoader resourceLoader;
    private GraphHopper hopper;
    private String versionId;
    private EdgeFilter snapFilter;

    public GraphHopperRoadRouter(RoutingProperties props, ResourceLoader resourceLoader) {
        this.props = props;
        this.resourceLoader = resourceLoader;
    }

    private synchronized GraphHopper hopper() {
        if (hopper != null) {
            return hopper;
        }
        init();
        return hopper;
    }

    private void init() {
        Resource res = resourceLoader.getResource(props.osmLocation());
        if (!res.exists()) {
            throw new RoutingUnavailableException("Road snapshot not found: " + props.osmLocation());
        }
        byte[] bytes;
        try (InputStream in = res.getInputStream()) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new RoutingUnavailableException("Road snapshot cannot be read: " + e.getMessage(), e);
        }
        String sha;
        try {
            sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RoutingUnavailableException("SHA-256 is not available: " + e.getMessage(), e);
        }
        if (!sha.equalsIgnoreCase(props.expectedSha256())) {
            throw new RoutingUnavailableException("Road snapshot checksum mismatch: expected "
                    + props.expectedSha256() + ", found " + sha + " (" + props.osmLocation() + ").");
        }
        String version = props.versionPrefix() + sha.substring(0, 12);
        Path dir = Path.of(props.cacheDir()).toAbsolutePath().resolve(version + "-gh" + Constants.VERSION);
        Path osm = dir.resolve("roads.osm");
        try {
            Files.createDirectories(dir);
            if (!Files.exists(osm)) {
                Files.write(osm, bytes);
            }
        } catch (IOException e) {
            throw new RoutingUnavailableException("Road graph cache cannot be written to " + dir + ": "
                    + e.getMessage(), e);
        }
        GraphHopper gh = new GraphHopper();
        gh.setOSMFile(osm.toString());
        gh.setGraphHopperLocation(dir.resolve("graph").toString());
        gh.setEncodedValuesString("car_access|block_private=false, car_average_speed, road_access");
        gh.setProfiles(new Profile("car")
                .setCustomModel(GHUtility.loadCustomModelFromJar("car.json"))
                .setTurnCostsConfig(TurnCostsConfig.car()));
        gh.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));
        try {
            gh.importOrLoad();
        } catch (RuntimeException e) {
            gh.close();
            throw new RoutingUnavailableException("Road graph import failed for " + version
                    + " (delete " + dir + " to rebuild): " + e.getMessage(), e);
        }
        snapFilter = new DefaultSnapFilter(gh.createWeighting(gh.getProfile("car"), new PMap()),
                gh.getEncodingManager().getBooleanEncodedValue(Subnetwork.key("car")));
        versionId = version;
        hopper = gh;
        LOG.info("Road graph {} ready: {} nodes, {} edges", version, gh.getBaseGraph().getNodes(),
                gh.getBaseGraph().getEdges());
    }

    @Override
    public String roadGraphVersion() {
        hopper();
        return versionId;
    }

    @Override
    public String routingEngine() {
        return ENGINE;
    }

    @Override
    public double snapDistanceMeters(WgsPoint p) {
        Snap snap = hopper().getLocationIndex().findClosest(p.latitude(), p.longitude(), snapFilter);
        if (!snap.isValid()) {
            throw new RoadLegUnavailableException(RoadLegUnavailableException.Reason.POINT_NOT_ROUTABLE,
                    "no drivable road of " + versionId + " near (" + p.longitude() + ", " + p.latitude() + ")");
        }
        double dist = snap.getQueryDistance();
        if (dist > props.maxSnapDistanceMeters()) {
            throw new RoadLegUnavailableException(RoadLegUnavailableException.Reason.POINT_NOT_ROUTABLE,
                    String.format(Locale.ROOT, "nearest drivable road of %s is %.0f m away (limit %.0f m)",
                            versionId, dist, props.maxSnapDistanceMeters()));
        }
        return dist;
    }

    @Override
    public RoadLeg route(WgsPoint from, WgsPoint to) {
        GHResponse rsp = hopper().route(
                new GHRequest(from.latitude(), from.longitude(), to.latitude(), to.longitude()).setProfile("car"));
        if (rsp.hasErrors()) {
            throw new RoadLegUnavailableException(RoadLegUnavailableException.Reason.NO_ROAD_PATH,
                    rsp.getErrors().get(0).getMessage());
        }
        ResponsePath p = rsp.getBest();
        PointList pts = p.getPoints();
        List<WgsPoint> geometry = new ArrayList<>(pts.size());
        for (int i = 0; i < pts.size(); i++) {
            geometry.add(new WgsPoint(pts.getLon(i), pts.getLat(i)));
        }
        return new RoadLeg(p.getDistance(), p.getTime() / 1000.0, geometry);
    }

    @Override
    public synchronized void destroy() {
        if (hopper != null) {
            hopper.close();
        }
    }
}
