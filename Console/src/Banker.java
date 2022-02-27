public class Banker {

    private int amountAccount = 0;
    private String clientName = "";

    public void getConsoleMenu() {

        System.out.println("*********************************************************************" );

        System.out.println("**         Caro cliente scelga dal menu quello di cui ha bisogno : **");
        System.out.println("**         1) Vedi importo conto corrente                          **");
        System.out.println("**         2) Preleva Contanti                                     **");
        System.out.println("**         3) Versa Contanti                                       **");
        System.out.println("**         4) Storico movimenti                                    **");

        System.out.println("*********************************************************************" );
    }

    public int getAmountAccount()
    {
        return amountAccount;
    }

    public void setAmountAccount(int newAmountAccount)
    {
        this.amountAccount = newAmountAccount;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
