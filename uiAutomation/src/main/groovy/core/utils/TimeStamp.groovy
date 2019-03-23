package core.utils

class TimeStamp {

    static String getTodaysDate(String format = "MMM dd, yyyy hh:00 a"){
        return new Date().format(format)
    }

    static String getNextYearDate(String format = "MMM dd, yyyy hh:00 a"){
        return new Date().plus(365).format(format)
    }
}
