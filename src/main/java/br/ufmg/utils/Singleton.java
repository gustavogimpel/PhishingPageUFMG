package br.ufmg.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import br.ufmg.app.Configuration;


public class Singleton{
	static private Singleton _instance;
	private Map<String,List<Long>> dicionarioRequisicoes;
	private long tempoInicio;
	private Path outputDir;
	private int janela_requisicoes;
	private int limite_requisicoes;
	// methods and attributes for Singleton pattern
	private Singleton() {initGlobals();}

	private void initGlobals() {
		dicionarioRequisicoes = new HashMap<String,List<Long>>();
		tempoInicio = System.currentTimeMillis();
	}

	public static Singleton getInstance() {
		if (_instance == null) {
			synchronized(Singleton.class) {
				if (_instance == null)_instance = new Singleton();
			}
		}
		return _instance;
	}

	// metodos e atributos globais
	synchronized public void setParameters(int janela_requisicoes,int limite_requisicoes, Path outputDir) {
		this.outputDir = outputDir;
		this.janela_requisicoes = janela_requisicoes;
		this.limite_requisicoes = limite_requisicoes;
	}

	synchronized public boolean isInDict(String dominio) {
		return dicionarioRequisicoes.containsKey(dominio);
	}

	synchronized public int getNumeroReq(String dominio) {
		if(((System.currentTimeMillis() - tempoInicio))/1000 > janela_requisicoes) {
			printHighestScores();
		}
		Predicate<Long> timePassed = timeBefore -> (((System.currentTimeMillis() - timeBefore))/1000 > janela_requisicoes);
		dicionarioRequisicoes.get(dominio).removeIf(timePassed);
		return dicionarioRequisicoes.get(dominio).size();
	}

	synchronized public void setNumeroReq(String dominio,long valor) {
		if (isInDict(dominio)) {
			dicionarioRequisicoes.get(dominio).add(valor);
		}else {
			List<Long> novaLista = new ArrayList<Long>();
			novaLista.add(valor);
			dicionarioRequisicoes.put(dominio, novaLista);
		}
	}

	synchronized public void printHighestScores() {
		List<Par> listaDomReq = new ArrayList<Par>();
		for(Map.Entry<String,List<Long>> entry : dicionarioRequisicoes.entrySet()) {
			List<Long> listaReqs = entry.getValue();
			for(Long req : listaReqs) {
				Long res = req - tempoInicio;
				Par temp = new Par(entry.getKey(),res.floatValue()/1000);
				listaDomReq.add(temp);
			}
		}
		Collections.sort(listaDomReq);
		try {
			FileWriter outputFile = new FileWriter(this.outputDir.resolve("requisicoes"),false);
			for (Par par:listaDomReq) {
				outputFile.write(par.primeiroValor()+"  "+par.segundoValor()+"\n");
			}
			outputFile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
}
