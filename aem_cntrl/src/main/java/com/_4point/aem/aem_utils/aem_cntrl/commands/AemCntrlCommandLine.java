package com._4point.aem.aem_utils.aem_cntrl.commands;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;

@Component
@Command(name = "aem-cntrl", subcommands = { InstallCommand.class } )
public class AemCntrlCommandLine {

}
