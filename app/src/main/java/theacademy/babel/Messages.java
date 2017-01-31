package theacademy.babel;

import java.util.Date;

/**
 * Created by Prosen on 4/6/2016.
 */
public class Messages {
    private String mtext ;
    private String mSender;
    private Date mDate;

    public Date getmDate(){

        return mDate;

    }

    public void setmDate(Date mDate){

        this.mDate = mDate;

    }


    public String getmSender(){

        return mSender;

    }

    public void setmSender(String mSender){

      this.mSender = mSender;

    }

    public String getmText(){

        return mtext;

    }

    public void setmText(String mtext){

        this.mtext = mtext;

    }



}
