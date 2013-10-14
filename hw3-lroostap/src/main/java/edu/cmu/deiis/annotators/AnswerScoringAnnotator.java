package edu.cmu.deiis.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.cas.FSIndex;
import org.cleartk.ne.type.NamedEntity;
import org.cleartk.ne.type.NamedEntityMention; 

import edu.cmu.deiis.types.*;

/**
 * 
 * Calculates score for each answer based on the Ngram matching algorithm and prints the results.
 * I added another part to this descriptor which considers NamedEntityMention option and applies 
 * this option in calculating the score for the answers.
 * @author Laleh
 *
 */
public class AnswerScoringAnnotator extends JCasAnnotator_ImplBase {

  /**
   * Calculates score for each answer based on an Ngram matching algorithm.
   */
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> nGramIter = aJCas.getAnnotationIndex(NGram.type).iterator();
    /**
     * creating a HashMap to link a question/answer to a list of NGrams 
     */
    HashMap<Annotation, ArrayList<NGram>> nGramHashMap = new HashMap<Annotation, ArrayList<NGram>>();
    Annotation question = null;

    while (nGramIter.hasNext()) {
        NGram nGram = (NGram) nGramIter.next();
        Annotation owner = nGram.getOwner();
        if (nGramHashMap.get(owner) == null) {
          nGramHashMap.put(owner, new ArrayList<NGram>());
        }
        nGramHashMap.get(owner).add(nGram);
        if (question == null && owner.getClass() == Question.class) {
          question = owner;
        }
      
    }

    ArrayList<NGram> questionNGramArrayList = nGramHashMap.get(question);
    Set<Annotation> annotationKeyList = nGramHashMap.keySet();
    annotationKeyList.remove(question);
    

    Iterator<Annotation> qIter = aJCas.getAnnotationIndex(Question.type).iterator();
    int questionBegin = 0;
    int questionEnd = 0;
    while(qIter.hasNext()){
      Question q = (Question)qIter.next();
      questionBegin = 0;
      questionEnd = q.getEnd();
    }
    

    // Finding the name entities in questions
    int questionNE = 0;
    FSIndex ne_index = aJCas.getAnnotationIndex(NamedEntityMention.type);
    //System.out.println("NamedEntity ----> ");
    //System.out.println(ne_index);
    Iterator neIt = ne_index.iterator();
    HashMap<String,Integer> mapENType = new HashMap<String,Integer>();
    //System.out.println("hasNext");
    //System.out.println(neIt.hasNext());
    while(neIt.hasNext()){
      NamedEntityMention nEM = (NamedEntityMention)neIt.next();
      //System.out.println("I am here");
      if(nEM.getBegin() >= questionBegin && nEM.getEnd() <= questionEnd){ 
        if(nEM.getMentionType()!=null){
          mapENType.put(nEM.getMentionType(), 0);
          questionNE++;
        }
      }
      if(nEM.getBegin() > questionEnd)
        break;
    }
    
    //System.out.println("question:");
    //System.out.println(questionNE);
    
    ArrayList<AnswerScore> answerScoreList = new ArrayList<AnswerScore>();
    ArrayList<Double> ngramScoreList = new ArrayList<Double>();
    ArrayList<Integer> answerNE = new ArrayList<Integer>(); 
    int answerMaxNE = 0; 
    /**
     * A simple NGram matching technique between a question and an answer
     */
    //System.out.println("siiiiiiize");
    //System.out.println(annotationKeyList.size());
    for (Annotation answer : annotationKeyList) {
      float numberOfMatches = (float) 0;
      float totalNumber = (float) 0;
      for (NGram answerNGram : nGramHashMap.get(answer)) {
        totalNumber = totalNumber + answerNGram.getN();
        for (NGram questionNGram : questionNGramArrayList) {
          if (questionNGram.getCoveredText().equals(answerNGram.getCoveredText())) {
            numberOfMatches = numberOfMatches + answerNGram.getN();
            break;
          }
        }
      }
      
    //Checking for the named entities in answers to match with the question
      neIt  = ne_index.iterator(); 
      int answerNEType = 0;
      while(neIt.hasNext()){
        NamedEntityMention nEM = (NamedEntityMention)neIt.next();
        if(nEM.getBegin()>=answer.getBegin() && nEM.getEnd()<= answer.getEnd()){
          if(nEM.getMentionType()!=null){
            if(mapENType.get(nEM.getMentionType())!=null){
              answerNEType++;
            }
          }
        }
        if(nEM.getBegin() > answer.getEnd())
          break;
      }
      answerNE.add(answerNEType);
      if(answerNEType > answerMaxNE)
        answerMaxNE = answerNEType;
      
      //System.out.println(answerNEType);
      //System.out.println("and max");
      //System.out.println(answerMaxNE);
      /**
       * Creating an AnswerScore and set the variables
       */
      AnswerScore ansScore = new AnswerScore(aJCas);
      ansScore.setCasProcessorId(this.getClass().getName());
      //System.out.println(answer.getType());
      //System.out.println(answer.getCoveredText());
      ansScore.setAnswer((Answer) answer);
      ansScore.setBegin(answer.getBegin());
      ansScore.setEnd(answer.getEnd());
      ansScore.setConfidence(1);
      //ansScore.setScore((float) numberOfMatches / totalNumber);
      answerScoreList.add(ansScore);
      ngramScoreList.add((double) numberOfMatches / totalNumber);
    
    }
    
    
    for(int answer=0;answer<answerScoreList.size();answer++){
      AnswerScore ansSc = answerScoreList.get(answer);
      double neScore = 0;
      double combinedScore = 0;
      double ngramScore = ngramScoreList.get(answer);
      neScore = (double)answerNE.get(answer)/answerMaxNE;
      //System.out.println(answerNE.get(answer));
      combinedScore = (ngramScore + neScore)/2;
      ansSc.setScore(combinedScore);
      ansSc.addToIndexes();
    }
     
  }
  
  

}
