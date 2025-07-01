package com._4point.aem.aem_utils.aem_cntrl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com._4point.aem.aem_utils.aem_cntrl.commands.AemCntrlCommandLine;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
public class AemCntrlApplication implements CommandLineRunner, ExitCodeGenerator {

	private final IFactory factory;
	private final AemCntrlCommandLine aemCntrlCommandLine;
	private int exitCode;

	public AemCntrlApplication(IFactory factory, AemCntrlCommandLine aemCntrlCommandLine) {
		this.factory = factory;
		this.aemCntrlCommandLine = aemCntrlCommandLine;
	}

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(AemCntrlApplication.class, args)));
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public void run(String... args) throws Exception {
		// let picocli parse command line args and run the business logic
        exitCode = new CommandLine(aemCntrlCommandLine, factory).execute(args);
    }
}
