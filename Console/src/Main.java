import java.util.Scanner;

class Main {

    public static void main(String arg[])
    {
        Scanner inputFromConsole = new Scanner(System.in);
        Banker accountManager = new Banker();

        int selectedOption = 0;
        int valueModification = 0;
        boolean isInterrupted = true;
        String toContinue = "";

        System.out.println(" Caro cliente inserisca il proprio nome : " );
        accountManager.setClientName( inputFromConsole.nextLine());
        accountManager.getConsoleMenu();

        while(isInterrupted)
        {

            System.out.println(" Caro "+ accountManager.getClientName() +" scelga l'opzione di cui ha bisogno : " );

            selectedOption = Integer.parseInt(inputFromConsole.nextLine());
            System.out.println("Caro "+ accountManager.getClientName() +" ha selezionato l'opzione " + selectedOption);

            switch (selectedOption)
            {

                case  1:
                    System.out.println( accountManager.getClientName() + " il tuo conto é a " + accountManager.getAmountAccount() + " euro ");
                    break;
                case  2:

                    System.out.println(" Ha scelto l'opzione preleva Contanti : Inserisca l'importo che vuole prelevare : ");
                    valueModification = Integer.parseInt(inputFromConsole.nextLine());

                    int modifiedValue = accountManager.getAmountAccount() - valueModification;

                    if( (modifiedValue) < 0 )
                    {
                        System.out.println( accountManager.getClientName() +" il tuo conto é a " + accountManager.getAmountAccount() + " euro ");
                        System.out.println(" Non puó prelevare l'importo richiesto di "+ valueModification+" perché é povero !!!" );
                    }else
                    {
                        accountManager.setAmountAccount(modifiedValue);
                        System.out.println(" Il nuovo totale del suo conto é " + accountManager.getAmountAccount());
                    }
                    break;

                case  3:
                    System.out.println( " Ha scelto l'opzione Versa Contanti : Inserisca l'importo che vuole aggiungere :");
                    valueModification = Integer.parseInt(inputFromConsole.nextLine());
                    accountManager.setAmountAccount(accountManager.getAmountAccount() + valueModification) ;
                    System.out.println(" Il nuovo totale del suo conto é " + accountManager.getAmountAccount() + " euro");
                    break;

                case 4:
                    System.out.println(" La funzione storico movimenti non é disponibile ");
                    break;
            }

            System.out.println("Caro "+ accountManager.getClientName() +" vuole eseguire un altra operazione ? Inserisca Si o No ");
            toContinue = inputFromConsole.nextLine();

            if(toContinue.equals("No"))
            {
                isInterrupted = false;
            }

        }
    }
}

