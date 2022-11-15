package br.ufmg.app;

public class Main {
	public static void main(String[] args) {

		if (args.length > 1) {
			System.err.println("The only required parameter is the configuration filepath.");
			System.exit(-1);
		} else if (args.length < 1) {
			System.err.println("Required execution argument: path of the json configuration file.");
			System.exit(-1);
		}

		Configuration config = new Configuration(args[0]);
		// TODO: Print the JSON configuration

		App app = new App(config);

		try {
			app.run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.exit(0);
	}
}
