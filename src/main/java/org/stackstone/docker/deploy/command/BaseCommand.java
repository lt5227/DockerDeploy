package org.stackstone.docker.deploy.command;

/**
 * BaseCommand
 *
 * @author Lt5227
 * @date 2021/9/15
 * @since 1.0.0
 */
public interface BaseCommand {
    /**
     * Build the docker image and run it according to the options.
     */
    void build();
}
