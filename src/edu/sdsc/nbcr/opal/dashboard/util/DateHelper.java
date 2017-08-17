package edu.sdsc.nbcr.opal.dashboard.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides some functionalities that are helpful when dealing with dates.
 * 
 * @author clem
 *
 */
public class DateHelper {

    static final int MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;
    protected static Log log = LogFactory.getLog(DateHelper.class.getName());
    
    /**
     * This function returns (date - 1day).
     */
    public static Date subtractDay(Date date){
        return addDays(date, -1);
    }
    
    /**
     * This function returns (date - numDays).
     */
    public static Date addDays(Date date, int numDays){
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date);
        //log.debug("The date before subctracting one day is: " + date);
        cal1.add(Calendar.DAY_OF_MONTH, numDays);
        //log.debug("The date after subtracting one day is: " + cal1.getTime());
        return cal1.getTime();
    }
    
    /**
     * This functions subtract a month form the date.
     * @param date the input date
     * @return the input date minus one month
     * 
     */
    public static Date subtractMonth(Date date){
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date);
        //log.debug("The date before subctracting one day is: " + date);
        cal1.add(Calendar.MONTH, -1);
        return cal1.getTime();
    }
    
    /**
     * It tries to parse the string date following the format MM/dd/yy.
     * It return null if it can not parse the date.
     * 
     * @param date the string to be parsed
     * @return the date parsed
     */
    public static Date parseDate(String date) throws java.text.ParseException {
        if ( date == null) throw new java.text.ParseException("null is not a valid value", 1);
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy", Locale.US);
        Date returnDate = null;
        returnDate = formatter.parse(date);
        return returnDate;
    }
    
    /**
     * It tries to parse the string date following the format yyyy MM dd.
     * It return null if it can not parse the date.
     * 
     * @param date the string to be parsed
     * @return the date parsed
     */
    public static Date parseDateWithSpaces(String date){
        if ( date == null) return null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy MM dd", Locale.US);
        Date returnDate = null;
        try {
            returnDate = formatter.parse(date);
        }
        catch (Exception e){
            log.error("Impossible to parse date: " + date, e );
        }
        return returnDate;
    }
    

    public static String formatDate(Date date){
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy", Locale.US);
        return formatter.format(date);
    }
    
    /**
     * Return true if d1 and d1 are on the same day
     * 
     * @param d1
     * @param d2
     * @return true if d1 d2 are on the same day
     */
    public static boolean  compareDates(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);
        
        if ( (cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)) && 
                ( cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)) && 
                (cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)) ){
            log.debug("Date comparison with " + d1 + " and " + d2 + " retunring true");
            return true;
        }
        log.debug("Date comparison with " + d1 + " and " + d2 + " retunring false");
        return false;
    }
    
    
    /**
     * This function returns the number of days between date1 and date2
     * @param date1
     * @param date2
     * @return the number of days between date1 and date2
     * 
     */
    public static int getOffsetDays(Date date1, Date date2){
        long diff = date1.getTime() - date2.getTime();
        long numberOfDays =  diff / MILLISECONDS_PER_DAY;
        //log.info("the differnce between: " + date1 + " and " + date2 + " is: " + numberOfDays);
        return (int)numberOfDays;
    }

    /**
     * Return true if (d1 + 1day) == d2
     * @param d1
     * @param d2
     * 
     */
    public static boolean isOneDayOffset(Date d1, Date d2){
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        cal1.add(Calendar.DAY_OF_MONTH, 1);
        return compareDates(cal1.getTime(), d2);
    }//isOneDayOffset
    
    /**
     * Return the default start date that is (today - 31)
     * 
     */
    public static Date getStartDate(){
        Date toDay = new Date();
        //toDay = DateHelper.subtractDay(toDay);
        return DateHelper.subtractMonth(toDay);
    }
    
    /**
     * Return the default end date that is yesterday
     * 
     */
    public static Date getEndDate(){
        //return DateHelper.subtractDay(new Date());
        return new Date();
    }
    
    /**
     * 
     * Return true if the value is contained in str.
     * This should not be here... but I just didn't know where to put it
     */
    public static boolean containsString(String [] str, String value){
        for ( int i = 0; i < str.length; i++ ) {
            if ( str[i].equals(value) )
                return true;
        }
        return false;
    }
    
}
