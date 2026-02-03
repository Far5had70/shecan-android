package ir.shecan.core.constant;

import ir.shecan.BuildConfig;

public class Constant {

    public static final String CafeBazaar = "CafeBazaar";
    public static final String Myket = "Myket";

    public static final String Store = BuildConfig.STORE;

    public static final boolean IsMyketMode = Store.equals(Myket);
    public static final boolean IsCafeBazaarMode = Store.equals(CafeBazaar);
    public static String BaseAuthRedirect = "https://my.shecan.ir/panel/auth?token=%s&url=%s";

    public static String TransactionUrlRaw = "https://my.shecan.ir/panel/transactions";
    public static String TicketUrlRaw = "https://my.shecan.ir/panel/support";
    public static String DomainUrlRaw = "https://my.shecan.ir/panel/domains";
    public static String OrderWebPageUrlRaw = "https://my.shecan.ir/panel/orders";
    public static String PlanUrl = "https://shecan.ir/";
}