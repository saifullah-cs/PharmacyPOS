package com.mycompany.pharmacypos;

public class PharmacyPOS {

    public PharmacyPOS(String role) {

        new HomeFrame(role);

    }

    public static void main(String[] args) {

    LoginFrame.loggedInUser = "Developer";

    new HomeFrame("admin");
    

}

}