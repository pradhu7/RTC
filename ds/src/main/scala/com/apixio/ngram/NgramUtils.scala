package com.apixio.ngram

import java.util.StringTokenizer
import scala.collection.JavaConversions._

object NgramUtils {

  val MIN_WORDS_IN_PHARSE = 1
  val MAX_WORDS_IN_PHARSE = 1

  val MIN_CHARS_IN_NGRAM = 2
  val MAX_CHARS_IN_NGRAM = 20

  // input: str: string to be ngram-ed
  // output: list of ngram with associated score List(ngram1->score1, ngram2->score2)
  // output: List(score1:=:ngram1, score2:=:ngram2
  def generate(str: String): List[String] = {

    val ngrams = NGramsGenerator.generate(str.toLowerCase, MIN_WORDS_IN_PHARSE, MAX_WORDS_IN_PHARSE)

    ngrams.map(l =>
      l.flatMap(str =>

      {

        val chars = str.toCharArray.toList

        val maxNgramLength = if (MAX_CHARS_IN_NGRAM > chars.length) chars.length else MAX_CHARS_IN_NGRAM
        val minNgramLength = if (MIN_CHARS_IN_NGRAM > chars.length) chars.length else MIN_CHARS_IN_NGRAM

        var list = List[String]()

        for (i <- minNgramLength to maxNgramLength)
          list ::= f"${(i.toDouble/maxNgramLength)*1000}%4.0f".replace(" ","0") + ":=:" + chars.slice(0, i).mkString

        list
      }
      )
    ).flatten.toSet.toList
  }

  def getNgramStrJ(str: String): java.util.List[ ( String, String )]  =
    getNgramStr(str)

  def getNgramStr(str: String): List[ ( String, String )] =
    NgramUtils.generate(str)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(n => ( n.split(":=:").head, n.split(":=:").tail.mkString))
      .map(n => (n._1, n._2))

  def getNgramStrJ(listStr: List[String]): java.util.List[ ( String, String )] =
    getNgramStr(listStr)

  def getNgramStr(listStr: List[String]): List[ ( String, String )] =
    listStr.flatMap(NgramUtils.generate)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(n => (n.split(":=:").head, n.split(":=:").tail.mkString))
      .map(n => (n._1, n._2))

  /*
  def getHashStr(s: String) = {
    //@Todo change to Murmur Hash3 for production
    //MurmurHash3.stringHash(s).toString
    s
  }*/
}


object NGramsGenerator
{
  // each ngram is represented as a List of String
  def generate(text : String, minSize : Int, maxSize : Int) =
  {
    assert(maxSize >= minSize && minSize > 0 && maxSize > 0)

    // iterate for each token on the available ngrams
    for (ngramList <- this.generate_sub(text, minSize, maxSize);
         ngram <- ngramList)
      yield ngram.toList
  }

  // generate a list of ngrams
  def generate_sub(text : String, minSize : Int, maxSize : Int) =
  {
    val nGramsBuffer = new NGramsBuffer(minSize, maxSize)
    for (t <- this.getTokenizerIterator(text))
      yield
      {
        nGramsBuffer.addToken(t.asInstanceOf[String])
        nGramsBuffer.getNGrams()
      }
  }

  // Can be overloaded to use an other tokenizer
  def getTokenizerIterator(text : String) =
    new StringTokenizer(text)

  // A utility class that stores a list of fixed size queues, each queue is a current ngram
  class NGramsBuffer(val minSize : Int, val maxSize : Int)
  {
    val queues = minSize.to(maxSize).foldLeft(List[SlidingFixedSizeQueue[String]]()) {
      (list, n) =>
        new SlidingFixedSizeQueue[String](n) :: list
    }

    def addToken(token : String) = queues.foreach(q => q.enqueue(token))

    // return only buffers that are full, otherwise not enough tokens have been
    // see yet to fill in the NGram
    def getNGrams() = queues.filter(q => q.size == q.maxSize)
  }

  // Utility class to store the last maxSize encountered tokens
  class SlidingFixedSizeQueue[A](val maxSize : Int) extends scala.collection.mutable.Queue[A]
  {
    override def enqueue(elems : A*) =
    {
      elems.foreach(super.enqueue(_))

      while (this.size > this.maxSize)
        this.dequeue()
    }
  }

}
