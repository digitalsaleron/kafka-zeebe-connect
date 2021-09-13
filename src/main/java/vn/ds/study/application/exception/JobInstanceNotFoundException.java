/*
 * Class: EntryNotFoundException
 *
 * Created on Sep 11, 2021
 *
 * (c) Copyright Swiss Post Solutions Ltd, unpublished work
 * All use, disclosure, and/or reproduction of this material is prohibited
 * unless authorized in writing.  All Rights Reserved.
 * Rights in this program belong to:
 * Swiss Post Solution.
 * Floor 4-5-8, ICT Tower, Quang Trung Software City
 */
package vn.ds.study.application.exception;

public class JobInstanceNotFoundException extends Exception{

    private static final long serialVersionUID = -5572716942518329826L;

    public JobInstanceNotFoundException() {
        
    }
    
    public JobInstanceNotFoundException(String msg) {
        super(msg);
    }
}
