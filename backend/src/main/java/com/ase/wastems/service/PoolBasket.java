package com.ase.wastems.service;

/** One real DSNY basket of the MN01 snapshot that the experiment generator may sample. */
public record PoolBasket(String basketId, String name, double longitude, double latitude) {}
