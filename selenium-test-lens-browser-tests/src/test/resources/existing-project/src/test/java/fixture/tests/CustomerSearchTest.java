package fixture.tests;

import fixture.pages.CustomersPage;
import org.testng.annotations.Test;

public final class CustomerSearchTest {
    private CustomersPage customersPage;

    @Test(groups = {"customers"})
    public void searchesCustomers() {
        customersPage.search("Ada");
    }
}
