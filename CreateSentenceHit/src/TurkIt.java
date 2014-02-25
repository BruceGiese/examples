/*
 * Copyright 2007-2012 Amazon Technologies, Inc.
 * Additional code by Bruce Giese (diff this to Amazon's SimpleSurvey code to see what)
 * 		This additional code is covered by the same Apache2 license.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */ 

import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.requester.Comparator;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.Locale;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;

/**
 * Submit a an XML file to be a Mechanical Turk Hit.
 * 
 * You need to add java-aws-mturk.jar to your class path.
 * You need to add aws-mturk-wsdl.jar
 * 
 * mturk.properties must be found in the current file path.
 * 
 */
public class TurkIt {

  private RequesterService service;
	
  //	Default attributes if nothing is given via the setter methods
  private String title = "Generic HIT";
  private String description = "This is a generic Mechanical Turk HIT.";
  private int numAssignments = 1;
  private double reward = 0.01;
  private String keywords = "sample, generic";
  private long assignmentDurationInSeconds = 60 * 60;			// 1 hour
  private long autoApprovalDelayInSeconds = 60 * 60 * 24 * 15;	// 15 days
  private long lifetimeInSeconds = 60 * 60 * 24 * 3;			// 3 days
  private String requesterAnnotation = "sample#hit";
  
  //Defining the location of the externalized question (QAP) file.
  private String questionFile = "./simple_survey.question";
  private String propertiesFile = "./mturk.properties";
  
  /**
   * Constructor
   */
  public TurkIt(String propertiesFile) {
	  this.propertiesFile = propertiesFile;
	  service = new RequesterService(new PropertiesClientConfig(propertiesFile));
  }
  
  
  /**
   * Setter for adding parameters, string these together like param1("Title").param2(42).param3("xyzzy")
   */
  public TurkIt title(String title) {
	  this.title = title;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt description(String description) {
	  this.description = description;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt numAssignments(int numAssignments) {
	  this.numAssignments = numAssignments;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt reward(double reward) {
	  this.reward = reward;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt keywords(String keywords) {
	  this.keywords = keywords;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt assignmentDurationInSeconds(long assignmentDurationInSeconds) {
	  this.assignmentDurationInSeconds = assignmentDurationInSeconds;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt autoApprovalDelayInSeconds(long autoApprovalDelayInSeconds) {
	  this.autoApprovalDelayInSeconds = autoApprovalDelayInSeconds;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt lifetimeInSeconds(long lifetimeInSeconds) {
	  this.lifetimeInSeconds = lifetimeInSeconds;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt requesterAnnotation(String requesterAnnotation) {
	  this.requesterAnnotation = requesterAnnotation;
	  return this;
  }
  /**
   * Setter for adding parameters
   */
  public TurkIt questionFile(String questionFile) {
	  this.questionFile = questionFile;
	  return this;
  }
	
  
  /**
   * Checks to see if there are sufficient funds in your account.
   * @return true if there are sufficient funds.  False if not.
   */
  public boolean hasEnoughFund() {
    double balance = service.getAccountBalance();
    System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
    return balance > 0;
  }
  public boolean hasEnoughFund(double needed) {
	    double balance = service.getAccountBalance();
	    System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
	    return balance > needed;
	  }
  
  /**
   * Creates the simple survey.
   *
   */
  public void createHit() {
    try {

      // No qualification requirements for writing a sentence
      // TODO Add a qualification for voting that excludes those who wrote the sentences.
      QualificationRequirement[] qualReqs = null;
      qualReqs = new QualificationRequirement[] {  };

      // Loading the question (QAP) file. HITQuestion is a helper class that
      // contains the QAP of the HIT defined in the external file. This feature 
      // allows you to write the entire QAP externally as a file and be able to 
      // modify it without recompiling your code.
      HITQuestion question = new HITQuestion(questionFile);
      
      //Creating the HIT and loading it into Mechanical Turk
      HIT hit = service.createHIT(null, // HITTypeId 
          title, 
          description, keywords, 
          question.getQuestion(),
          reward, assignmentDurationInSeconds,
          autoApprovalDelayInSeconds, lifetimeInSeconds,
          numAssignments, requesterAnnotation, 
          qualReqs,
          null // responseGroup
        );
      
      System.out.println("Created HIT: " + hit.getHITId());

      System.out.println("You may see your HIT with HITTypeId '" 
          + hit.getHITTypeId() + "' here: ");
      
      System.out.println(service.getWebsiteURL() 
          + "/mturk/preview?groupId=" + hit.getHITTypeId());
      
      //Demonstrates how a HIT can be retrieved if you know its HIT ID
      HIT hit2 = service.getHIT(hit.getHITId());
      
      System.out.println("Retrieved HIT: " + hit2.getHITId());
      
      if (!hit.getHITId().equals(hit2.getHITId())) {
        System.err.println("The HIT Ids should match: " 
            + hit.getHITId() + ", " + hit2.getHITId());
      }
      
    } catch (Exception e) {
      System.err.println(e.getLocalizedMessage());
    }
  }
}
