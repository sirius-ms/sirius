package de.unijena.bioinf.sirius.gui.compute;

import java.util.*;

import de.unijena.bioinf.myxo.structure.CompactPeak;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

public class MassSearchable implements Searchable<String, String>{

	private HashMap<String,CompactPeak> peaks;
	
	public MassSearchable(ExperimentContainer ec){
		List<CompactSpectrum> ms1 = ec.getMs1Spectra();
		peaks = new HashMap<>();
		if(ms1==null || ms1.isEmpty()){
			//TODO
		}else{
			CompactSpectrum spec = ms1.get(0);
//			System.out.println(spec);
//			System.out.println(spec.getSize());
			for(int i=0;i<spec.getSize();i++){
//				System.out.println(spec.getMass(i)+" "+spec.getPeak(i));
//				System.out.println(String.valueOf(spec.getMass(i)));
				peaks.put(String.valueOf(spec.getMass(i)),spec.getPeak(i));
			}
		}
	}

	@Override
	public Collection<String> search(String value) {
		TreeSet<String> returns = new TreeSet<>();
		for(String mass : peaks.keySet()){
			if(mass.startsWith(value))returns.add(mass);
		}
		return returns;
	}

}
