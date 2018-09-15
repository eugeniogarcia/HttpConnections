package com.swisscom.hero.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.swisscom.heroes.model.Hero;

@ConfigurationProperties(prefix = "oce")
public class heroesDS {
	private static final int DEFAULT_DELAY= 0;

	private int retraso = DEFAULT_DELAY;

	public int getRetraso() {
		return this.retraso*1000;
	}

	public void setRetraso(int retraso) {
		this.retraso=retraso;
	}

	private final List<Hero> source;
	private int last;

	public heroesDS(){
		source=new ArrayList<Hero>();
		source.add(new Hero.Builder().id(1).name("Pupa").createHero());
		source.add(new Hero.Builder().id(2).name("Nani").createHero());
		source.add(new Hero.Builder().id(3).name("Mausi").createHero());
		source.add(new Hero.Builder().id(4).name("Nico").createHero());
		last=4;
	}

	public Hero add(Hero hero) {
		do {
			last++;
		}while(source.stream().anyMatch(x-> {return (last==x.getId());}));

		final Hero val=new Hero.Builder().id(last).name(hero.getName()).createHero();

		source.add(val);

		return val;
	}

	public boolean update(Hero hero) {
		final Optional<Hero> val=source.stream().filter((Hero x)-> x.getId()==hero.getId()).findFirst();

		if(val.isPresent()) {
			source.remove(val.get());
			source.add(hero);
			return true;
		}
		return false;
	}

	public boolean delete(Hero hero) {
		final Optional<Hero> val=source.stream().filter((Hero x)-> x.getId()==hero.getId()).findFirst();

		if(val.isPresent()) {
			source.remove(val.get());
			return true;
		}
		return false;
	}	

	public Hero find(int Id) {
		final Optional<Hero> val=source.stream().filter((Hero x)-> x.getId()==Id).findFirst();

		if(val.isPresent()) {
			return val.get();
		}
		return null;
	}	

	public List<Hero> list() {
		return source;
	}	
}
