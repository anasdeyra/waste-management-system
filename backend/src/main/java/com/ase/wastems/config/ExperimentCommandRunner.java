package com.ase.wastems.config;

import com.ase.wastems.domain.BatchStatus;
import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.service.AuditService;
import com.ase.wastems.service.ExperimentOutputWriter;
import com.ase.wastems.service.ExperimentService;
import com.ase.wastems.service.ExperimentSpec;
import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportService;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Headless experiment run (profile {@code experiment}, tools/run-experiment.sh, D-050): imports
 * MN01, runs the configured batch, writes the output files and exits (0 completed, 1 error,
 * 2 aborted). Never active in tests or in the normal application.
 */
@Component
@Profile("experiment")
@Order(10)
public class ExperimentCommandRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentCommandRunner.class);

    private final Mn01ImportService importer;
    private final ExperimentService experiments;
    private final ExperimentOutputWriter writer;
    private final ExperimentProperties props;
    private final ConfigurableApplicationContext context;

    public ExperimentCommandRunner(Mn01ImportService importer, ExperimentService experiments,
            ExperimentOutputWriter writer, ExperimentProperties props, ConfigurableApplicationContext context) {
        this.importer = importer;
        this.experiments = experiments;
        this.writer = writer;
        this.props = props;
        this.context = context;
    }

    static ExperimentSpec specFrom(ExperimentProperties p) {
        return ExperimentSpec.of(p.profiles(), p.seeds(), p.sizes(), p.repetitions(), p.label());
    }

    @Override
    public void run(ApplicationArguments args) {
        int code;
        try {
            if (props.outputDir() == null || props.outputDir().isBlank()) {
                throw new IllegalStateException("wastems.experiment.output-dir is not set");
            }
            ExperimentSpec spec = specFrom(props);
            LOG.info("Experiment run starting: {} runs, output {}", spec.runCount(), props.outputDir());
            long start = System.nanoTime();
            importer.importAll(ImportTrigger.STARTUP);
            EvaluationBatch batch = experiments.run(spec, AuditService.SYSTEM_ACTOR);
            writer.write(batch.getId(), Path.of(props.outputDir()));
            double seconds = (System.nanoTime() - start) / 1e9;
            LOG.info("Experiment batch {} {}: runs {}, completed {}, routing failed {}, {} s; output {}",
                    batch.getId(), batch.getStatus(), batch.getRunCount(), batch.getCompletedRunCount(),
                    batch.getFailedRunCount(), String.format(java.util.Locale.ROOT, "%.1f", seconds),
                    props.outputDir());
            code = batch.getStatus() == BatchStatus.COMPLETED ? 0 : 2;
        } catch (RuntimeException e) {
            LOG.error("Experiment run failed: {}", e.getMessage(), e);
            code = 1;
        }
        int exit = code;
        System.exit(SpringApplication.exit(context, () -> exit));
    }
}
