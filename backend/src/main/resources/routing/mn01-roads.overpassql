[out:xml][timeout:180];
(
  way["highway"~"^(motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential|living_street|service|road)$"](40.6950,-74.0250,40.7350,-73.9850);
  relation["type"="restriction"](40.6950,-74.0250,40.7350,-73.9850);
);
(._;>;);
out body;
