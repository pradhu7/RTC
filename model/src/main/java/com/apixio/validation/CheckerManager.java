package com.apixio.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.beanutils.PropertyUtils;

import com.apixio.model.patient.Patient;

public class CheckerManager {

    private Set<Class<?>> exclusion = new HashSet<Class<?>>();

    private Map<Class<?>, List<Checker<Object>>> checkerMap = new HashMap<Class<?>, List<Checker<Object>>>();

    public Map<Class<?>, List<Checker<Object>>> getCheckerMap() {
        return checkerMap;
    }

    public void setCheckerMap(Map<Class<?>, List<Checker<Object>>> checkerMap) {
        this.checkerMap = checkerMap;
    }

    public <T extends Object> CheckerManager addChecker(Class<T> k,
            Checker<?> checker) {

        if (!checkerMap.containsKey(k)) {
            List<Checker<Object>> mycheckers = new ArrayList<Checker<Object>>();

            mycheckers.add((Checker<Object>) checker);

            checkerMap.put(k, mycheckers);
        } else {
            checkerMap.get(k).add((Checker<Object>) checker);
        }

        return this;
    }

    public void addExclusion(Class<?> excluedClass) {
        exclusion.add(excluedClass);
    }

    public <T> List<Message> run(T root, String objectId) throws Exception {
        List<Message> result = new ArrayList<Message>();


        Stack<DecoratedObject> myStack = new Stack<DecoratedObject>();

        myStack.push(new DecoratedObject(root, objectId + "/" + root.getClass().getSimpleName()));

        while (!myStack.isEmpty()) {


            DecoratedObject curEntry = myStack.pop();
            if (curEntry.getObject() == null) {
                System.out
                    .println("entry in the stack is null, something is wrong");
                continue;
            }

            Class<?> curEntryType = curEntry.getObject().getClass();

            if (exclusion.contains(curEntryType))
                continue;

            if (checkerMap.containsKey(curEntryType)) {
                List<Checker<Object>> mycheckers = checkerMap.get(curEntryType);
                for (Checker<Object> curChecker : mycheckers) {

                    Object testObj = curEntry.getObject();
                    List<Message> cur = curChecker.check(testObj);

                    for (Message tmpMessage : curChecker.check(curEntry
                                .getObject())) {
                        result.add(new Message(curEntry.getPath() + ": "
                                    + tmpMessage.getMessage(), tmpMessage
                                    .getMessageType()));
                                }

                }
            } else {

                result.add(new Message("I don't find any checker for : "
                            + curEntryType.getName(), MessageType.ERROR));
            }

            Map<String, Object> myMap = PropertyUtils.describe(curEntry
                    .getObject());
            Iterator<Entry<String, Object>> it = myMap.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Object> myEntry = it.next();
                Object entryValue = myEntry.getValue();
                if (entryValue instanceof List<?>) {

                    int entryIdx = 0;
                    for (Object tmpEntryValue : (List) entryValue) {
                        myStack.add(new DecoratedObject(tmpEntryValue, curEntry
                                    .getPath()
                                    + "/"
                                    + myEntry.getKey()
                                    + "/"
                                    + entryIdx));
                        entryIdx++;
                    }
                } else {

                    if (entryValue != null) {

                        myStack.add(new DecoratedObject(entryValue, curEntry
                                    .getPath() + "/" + myEntry.getKey()));
                    }
                }
            }
        }
        return result;
    }

    public Set<Class<?>> getExclusion() {
        return exclusion;
    }

    public void setExclusion(Set<Class<?>> exclusion) {
        this.exclusion = exclusion;
    }
}
