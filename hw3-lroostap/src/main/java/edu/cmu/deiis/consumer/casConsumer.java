package edu.cmu.deiis.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.resource.ResourceProcessException;

import edu.cmu.deiis.types.*;

public class casConsumer  extends CasConsumer_ImplBase {
  
  @Override
  public void processCas(CAS aCAS) throws ResourceProcessException {
    // TODO Auto-generated method stub
    JCas aJCas;
    
    try {
      aJCas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }
    
    FSIterator<Annotation> ansScoreIter = aJCas.getAnnotationIndex(AnswerScore.type).iterator();
    
    ArrayList<AnswerScore> answerScoreList = new ArrayList<AnswerScore>();
    while(ansScoreIter.hasNext())
    {
      answerScoreList.add((AnswerScore)ansScoreIter.next());
    }
    
    
    
    /**
     * Sorting the answerScoreList according to the score
     */
      Comparator<AnswerScore> cmp = new Comparator<AnswerScore>() {
        public int compare(AnswerScore a, AnswerScore b) {
          return -Double.compare(a.getScore(), b.getScore());
        }
      };
      Collections.sort(answerScoreList, cmp);
      
      /**
       * printing the question
       */
      
      Iterator<Annotation> questionIter = aJCas.getAnnotationIndex(Question.type).iterator();
      if (questionIter.hasNext()){
        Question question = (Question) questionIter.next();
        System.out.print("Question: " + question.getCoveredText() + "\n");
      }
      

      
      /**
       * printing the answers with their scores
       */
      for (AnswerScore score : answerScoreList) {
        String isCorrect = "";
        if (score.getAnswer().getIsCorrect()){
          isCorrect = "+";
        }else{
          isCorrect = "-";
        }
        System.out.format("%s %.3f %s\n", isCorrect, score.getScore(), score.getCoveredText());
      }
      
      
      /**
       * calculating the precision at n where n is the number of correct answers
       */
      int correctAnswers = 0;
      for (int i = 0; i < answerScoreList.size(); ++i) {
        if (answerScoreList.get(i).getAnswer().getIsCorrect())
          ++correctAnswers;
      }
      
      int correctAnswerinN = 0;
      for (int i = 0; i < correctAnswers; ++i) {
        if (answerScoreList.get(i).getAnswer().getIsCorrect())
          ++correctAnswerinN;
      }
      System.out.format("Precision at %d : %.5f%n%n\n", correctAnswers, ((float) correctAnswerinN / correctAnswers));


  }
  

}
