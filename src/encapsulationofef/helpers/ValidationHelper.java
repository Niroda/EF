/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encapsulationofef.helpers;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains some methods that are used in reflection
 * @author Ali
 */
public class ValidationHelper {
    
    /**
     * Checks if given method name in given class type is valid setter, getter
     * @param methodName: method name to check
     * @param type: class type that contains given method
     * @return true if it's valid setter, getter otherwise false
     */
    public static boolean isValidSetterOrGetter(String methodName, Class<?> type) {
        String getterMethodName = 
                methodName.startsWith("get") ? 
                methodName : 
                (methodName.startsWith("set") ? methodName.replace("set", "get") : "get" + methodName);
        String setterMethodName = getterMethodName.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethodName, null);
            Method setter = type.getDeclaredMethod(setterMethodName, getter.getReturnType());
            return getter.getReturnType().getName().equals(setter.getParameters()[0].getType().getName());
        } catch (NoSuchMethodException ex) {
            //Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            //Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * checks if the return type of given method either is primitive or string
     * @param methodName: method name to check
     * @param type: class type that contains given method
     * @return true if it's primitive or string otherwise false
     */
    public static boolean isPremitive(String methodName, Class<?> type) {
        String getterMethodName = 
                methodName.startsWith("get") ? 
                methodName : 
                (methodName.startsWith("set") ? methodName.replace("set", "get") : "get" + methodName);
        String setterMethodName = getterMethodName.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethodName, null);
            Method setter = type.getDeclaredMethod(setterMethodName, getter.getReturnType());
            boolean result = getter.getReturnType().isPrimitive()
                            ||
                            getter.getReturnType().getName().contains("java.lang.String");
            return result;
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * checking relationship between given class and the return type of given method
     * @param methodName: method name to check its return type
     * @param type: class type that contains given method
     * @return true if given class has one to many to the return type of given method, otherwise false
     */
    public static boolean isOneToMany(String methodName, Class<?> type) {
        String getterMethodName = 
                methodName.startsWith("get") ? 
                methodName : 
                (methodName.startsWith("set") ? methodName.replace("set", "get") : "get" + methodName);
        String setterMethodName = getterMethodName.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethodName, null);
            Method setter = type.getDeclaredMethod(setterMethodName, getter.getReturnType());
            boolean result = !getter.getReturnType().isPrimitive()
                            &&
                            !getter.getReturnType().getName().contains("java.lang.String")
                            &&
                            !getter.getGenericReturnType().getTypeName().contains("java.util.List<");
            return result;
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * checking relationship between given class and the return type of given method
     * @param methodName: method name to check its return type
     * @param type: class type that contains given method
     * @return true if given class has many to one to the return type of given method, otherwise false
     */
    public static boolean isManyToOne(String methodName, Class<?> type) {
        String getterMethodName = 
                methodName.startsWith("get") ? 
                methodName : 
                (methodName.startsWith("set") ? methodName.replace("set", "get") : "get" + methodName);
        String setterMethodName = getterMethodName.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethodName, null);
            Method setter = type.getDeclaredMethod(setterMethodName, getter.getReturnType());
            String _getterNameFromRelationalClass = getter.getGenericReturnType().getTypeName();
            if(_getterNameFromRelationalClass.contains("java.util.List<")){
                _getterNameFromRelationalClass = _getterNameFromRelationalClass.split("java.util.List\\<")[1].replaceAll("\\>", "");
                Method[] _getterFromRelationalClass = Class.forName(_getterNameFromRelationalClass).getDeclaredMethods();
                for (int i = 0; i < _getterFromRelationalClass.length; i++) {
                    if (_getterFromRelationalClass[i].getName().startsWith("get")) {
                        String _fieldType =_getterFromRelationalClass[i].getGenericReturnType().getTypeName();
                        if(_fieldType.equals(type.getName()))
                            return true;
                    }
                }
            }
            
//            System.err.println(getter.getGenericReturnType().getTypeName()+ " from many to one => " + (!getter.getReturnType().isPrimitive()
//                            &&
//                            !getter.getReturnType().getName().contains("java.lang.String")
//                            &&
//                            getter.getGenericReturnType().getTypeName().contains("java.util.List")));
//            boolean result = !getter.getReturnType().isPrimitive()
//                            &&
//                            !getter.getReturnType().getName().contains("java.lang.String")
//                            &&
//                            getter.getGenericReturnType().getTypeName().contains("java.util.List<");
//            return result;
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
 
    /**
     * checking relationship between given class and the return type of given method
     * @param methodName: method name to check its return type
     * @param type: class type that contains given method
     * @return true if given class has many to many to the return type of given method, otherwise false
     */
    public static <T> boolean isManyToMany(String methodName, Class<?> type) {
        String getterMethodName = 
                methodName.startsWith("get") ? 
                methodName : 
                (methodName.startsWith("set") ? methodName.replace("set", "get") : "get" + methodName);
        String setterMethodName = getterMethodName.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethodName, null);
            // We won't use setter, it's just to make sure that given method which represents a specific field is readable and writable
            Method setter = type.getDeclaredMethod(setterMethodName, getter.getReturnType());
            
            boolean result = getter.getReturnType().isPrimitive()
                             ||
                             getter.getReturnType().getName().contains("java.lang.String")
                             ||
                             !getter.getGenericReturnType().getTypeName().contains("java.util.List<");
            if(result)
                return false;
            String mainClassFieldTypeName = getter.getGenericReturnType().toString();
            mainClassFieldTypeName = mainClassFieldTypeName.split("<")[1].replace(">", "");
            T relationalClass = (T) Class.forName(mainClassFieldTypeName).newInstance();
            Method[] _relationalClassMethods = relationalClass.getClass().getDeclaredMethods();
            for (int i = 0; i < _relationalClassMethods.length; i++) {
                if(_relationalClassMethods[i].getName().startsWith("get")
                        && isValidSetterOrGetter(_relationalClassMethods[i].getName(), relationalClass.getClass())) {
                    boolean checkReturnType = _relationalClassMethods[i].getGenericReturnType().getTypeName().contains("java.util.List");
                    if(checkReturnType) {
                        String returnType = _relationalClassMethods[i].getGenericReturnType().getTypeName().toString();
                        returnType = returnType.split("\\<")[1].replaceAll("\\>", "");
                        if(returnType.equals(type.getName()))
                            return true;
                    }
                }
            }
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ValidationHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * This function used in lambda parser to check if given Functional returns primitive type or string
     * @param functionalBody: given Functional body to check
     * @return true if it's primitive otherwise false
     */
    public static boolean isPrimitiveFunctional(String functionalBody) {
        return functionalBody.contains("java.lang.");
    }
}
