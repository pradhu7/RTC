model
=====

Apixio Patient Data Models

### maven build
```sh
mvn clean install
```

### generate dependency report
```sh
mvn project-info-reports:dependencies
```

### Use the old to new APO converter
```
    List<com.apixio.model.owl.interfaces.Patient> plist = new ArrayList<>();
    try {
        for (UUID u : patientIds) {
            com.apixio.model.patient.Patient p = dao.getPatient(u);
            OldAPOToNewAPO c = new OldAPOToNewAPO(p);
            com.apixio.model.owl.interfaces.Patient np = c.getNewPatient();
            plist.add(np);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
```