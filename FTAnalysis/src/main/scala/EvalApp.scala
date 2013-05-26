import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer

object EvalApp extends App{
  //new Evaluation(args(0)).fragmentProperties()
  //val n = new Normalization(args(0), ChemicalCompoundScorer.createMassDependendFormulaScorer(100, ChemicalCompoundScorer.createDefaultCompoundScorer(true)))
  //n.getMassDevsSd()
  //val s = ChemicalCompoundScorer.createDefaultCompoundScorer(true)
  //println(new Normalization("D:/daten/arbeit/analysis/results/merged", s).getNormalizationConstantForRoot)
  val ls = new LossSize(args(0))
  val pareto = ls.learnPareto(ls.commonLosses)
  println(ls.commonLosses.filter(l=>l.loss.getMass<50).map(l=>""+l.loss + " (" + l.loss.getMass + ")\t" + l.count ).mkString("\n"))
  val m = ls.commonLosses.filter(l=>l.loss.getMass<50).toSeq.sortBy(u=>u.count)
  println(m(m.size/2))
  println(ls.commonLosses.filter(l=>l.loss.getMass<50).filter(l=>l.count>4).size)
  /*
  print("xmin: ")
  println(pareto.getXmin)
  print("k: ")
  println(pareto.getK)
  val l = ls.learnCommonLosses()
  println(l.mkString("\n"))
  println("###############")
  val u = ls.learnUncommonLosses()
  println(u.mkString("\n"))
  val adjusted = ls.adjustCommonLosses()
  val p = ls.learnPareto(adjusted)
  print("xmin: ")
  println(p.getXmin)
  print("k: ")
  println(p.getK)
  */
}
