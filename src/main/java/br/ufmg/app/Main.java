package br.ufmg.app;

import br.ufmg.utils.Singleton;

public class Main {
	public static void main(String[] args) {

		if(args.length > 1) {
			System.err.println("The only required parameter is the configuration filepath.");
			System.exit(-1);
		} else if (args.length < 1) {
			System.err.println("Required execution argument: path of the json configuration file.");
			System.exit(-1);
		}

		Configuration config = new Configuration(args[0]);
		// TODO: Print the JSON configuration

		// System.out().setParameters(windowTimeout, maxRequestNumber);
		Singleton.getInstance().setParameters(config.getWindowTimeout(),
											  config.getMaxRequestNumber(),
											  config.getLogsDirPath());

		// App aplicacao = new App(concurrentBrowserInstancesNumber,
		// 						pageTimeout,
		// 						maxRequestNumber,
		// 						repository,
		// 						blackList,
		// 						whiteList,
		// 						logsDir);

		App aplicacao = new App(config);
		System.out.println("HERE");

		try {
			aplicacao.startLogFiles();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		aplicacao.obterArquivos();
		aplicacao.obterUrls();
		aplicacao.administrarProcessos();

	}
}
