/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encapsulationofef.helpers;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.UnsupportedDataTypeException;

/**
 * Contains some static functions that are used in reflection
 * @author Ali
 */
public class TypeHelper {

    /**
     * to get SETTER of passed string method name
     * @param methodName: desired method name
     * @param type: class type where it contains that method
     * @return Method
     */
    public static Method getSetterMethod(String methodName, Class<?> type) {
        String getterMethod
                = methodName.startsWith("set")
                ? methodName.replace("set", "get")
                : (methodName.startsWith("get") ? methodName : "get" + methodName);
        String setterMethod = getterMethod.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethod, null);
            return type.getDeclaredMethod(setterMethod, getter.getReturnType());
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * to get GETTER of passed string method name
     * @param methodName: desired method name
     * @param type: class type where it contains that method
     * @return Method
     */
    public static Method getGetterMethod(String methodName, Class<?> type) {
        String getterMethod
                = methodName.startsWith("set")
                ? methodName.replace("set", "get")
                : (methodName.startsWith("get") ? methodName : "get" + methodName);
        String setterMethod = getterMethod.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethod, null);
            // Even though we don't need setter but just to make sure,
            // that the field associated with the requested getter is writable as well!
            // If not basically this method will throw an exception :)
            Method setter = type.getDeclaredMethod(setterMethod, getter.getReturnType());
            return getter;
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * To get foreing key name
     * @param className: relational class to look in it
     * @param type: class type where it contains that reference
     * @return foreing key name
     */
    public static String getForeignKey(String className, Class<?> type) {
        try {
            Class<?> targetClass = Class.forName(type.getPackage().getName() + "." + className);
            Method[] methods = targetClass.getDeclaredMethods();
            for (Method m : methods) {
                String typeName = m.getGenericReturnType().getTypeName();
                String desiredType = type.getPackage().getName() + "." + type.getSimpleName();
                if (typeName.equals(desiredType)) {
                    return m.getName().replaceAll("get|set", "").toLowerCase();
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SelectQueriesHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new NoSuchElementException(className + " doesn't exists as reference in " + type.getSimpleName());
    }
    
    /**
     * It used by _fetchTheResults method to get new instnace of given method return type
     * @param methodName: method name to check its return type
     * @param type:       the class that contains given method
     * @return  an instance of given method return type
     */
    public static Object createNewInstanceByRelationalProperty(String methodName, Class<?> type) {
        String getterMethod
                = methodName.startsWith("set")
                ? methodName.replace("set", "get")
                : (methodName.startsWith("get") ? methodName : "get" + methodName);
        String setterMethod = getterMethod.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethod, null);
            Method setter = type.getDeclaredMethod(setterMethod, getter.getReturnType());
            String className = getter.getGenericReturnType().getTypeName()
                    .split(type.getPackage().getName() + ".")[1]
                    .replaceAll("(\\<|\\>|\\[|\\])", "");
            return Class.forName(type.getPackage().getName() + "." + className).newInstance();
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * retrieves class name from return type of given method
     * @param methodName: method name to check its return type
     * @param type: class type that contains given method
     * @return class name as string
     */
    public static String getClassName(String methodName, Class<?> type) {
        String getterMethod
                = methodName.startsWith("set")
                ? methodName.replace("set", "get")
                : (methodName.startsWith("get") ? methodName : "get" + methodName);
        String setterMethod = getterMethod.replace("get", "set");
        try {
            Method getter = type.getDeclaredMethod(getterMethod, null);
            Method setter = type.getDeclaredMethod(setterMethod, getter.getReturnType());
            String className = getter.getGenericReturnType().getTypeName()
                    .split(type.getPackage().getName() + ".")[1]
                    .replaceAll("(\\<|\\>|\\[|\\])", "");
            return className;
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(TypeHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Used in _fetchTheResults method to determine the return type of the method.
     * NOTE: primitives and string ONLY!
     * @param setMethod: is Method object
     * @return "PrimitiveTypes" enum
     * @throws UnsupportedDataTypeException
     *          if passed method is not primitive or string will throw this exception.
     */
    public static PrimitiveTypes getMethodType(Method setMethod) throws UnsupportedDataTypeException {
        Class<?> returnTypeName = setMethod.getParameters()[0].getType();
        if(returnTypeName.isPrimitive()
           || returnTypeName.getSimpleName().contains("String")) {
            if(returnTypeName.equals(int.class))
                return PrimitiveTypes.Int;
            else if(returnTypeName.equals(double.class))
                return  PrimitiveTypes.Double;
            else if(returnTypeName.equals(float.class))
                return  PrimitiveTypes.Float;
            else if(returnTypeName.equals(long.class))
                return  PrimitiveTypes.Long;
            else if(returnTypeName.equals(String.class))
                return  PrimitiveTypes.String;
            else if(returnTypeName.equals(short.class))
                return  PrimitiveTypes.Short;
            else if(returnTypeName.equals(boolean.class))
                return  PrimitiveTypes.Boolean;
        }
        throw new UnsupportedDataTypeException(setMethod.getReturnType().getSimpleName() + " is not supported!");
    }
    
    /**
     * to get SETTERS of passed class type
     * @param type desired class type to fetch its setters
     * @param withGetters: if you wish to get GETTERS as well set it to true otherwise false
     * @return List<Method>
     */
    public static List<Method> getSetterMethods(Class<?> type, boolean withGetters) {
        Method[] _tempMethods = type.getDeclaredMethods();
        List<Method> _methods = new ArrayList();
        for (Method m : _tempMethods) {
            if (!Modifier.isFinal(m.getModifiers())
                    && ValidationHelper.isValidSetterOrGetter(m.getName(), type)  
                    && (m.getName().startsWith("set") || withGetters)) {
                _methods.add(m);
            }
        }
        return _methods;
    }
    
    /**
     * to get GETTERS of passed class type
     * @param type desired class type to fetch its getters
     * @param withSetters: if you wish to get SETTERS as well set it to true otherwise false
     * @return List<Method>
     */
    public static List<Method> getGetterMethods(Class<?> type, boolean withSetters) {
        Method[] _tempMethods = type.getDeclaredMethods();
        List<Method> _methods = new ArrayList();
        for (Method m : _tempMethods) {
            if (!Modifier.isFinal(m.getModifiers()) 
                    && ValidationHelper.isValidSetterOrGetter(m.getName(), type) 
                    && (m.getName().startsWith("get") || withSetters)) {
                _methods.add(m);
            }
        }
        return _methods;
    }
    
}
