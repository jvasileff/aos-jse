/*
 * Copyright (c) 2002 John Vasileff
 *
 * Permission  is  hereby  granted,  free of  charge,  to  any  person
 * obtaining  a copy  of  this software  and associated  documentation
 * files   (the  "Software"),   to  deal   in  the   Software  without
 * restriction, including without limitation  the rights to use, copy,
 * modify, merge, publish, distribute,  sublicense, and/or sell copies
 * of the  Software, and  to permit  persons to  whom the  Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright  notice and  this permission  notice shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE  IS  PROVIDED  "AS   IS",  WITHOUT  WARRANTY  OF  ANY
 * KIND,  EXPRESS  OR  IMPLIED,  INCLUDING  BUT  NOT  LIMITED  TO  THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES  OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT,  TORT OR OTHERWISE, ARISING FROM, OUT  OF OR IN
 * CONNECTION WITH  THE SOFTWARE OR THE  USE OR OTHER DEALINGS  IN THE
 * SOFTWARE.
 */

package org.anodyneos.jse.cron;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
//import java.beans.IntrospectionException;

/**
 *  @version $Id: BeanUtil.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class BeanUtil {

    public static void set(Object obj, String name, String value, String type)
    throws StringCoerceException, JseIntrospectionException, InvocationTargetException, IllegalAccessException {
        Class clazz = obj.getClass();
        Method method = null;
        Method[] methods = clazz.getMethods();
        for(int i = 0; i < methods.length; i++) {
            String methodName = methods[i].getName();
            String matchName =
                        "set"
                    +   Character.toUpperCase(name.charAt(0))
                    +   name.substring(1);
            if(methodName.equals(matchName) || methodName.equals(matchName)) {
                Class[] paramTypes = methods[i].getParameterTypes();
                if (paramTypes.length == 1) {
                    Object arg = toArg(value, paramTypes[0], type);
                    if (arg != null) {
                        try {
                            methods[i].invoke(obj, new Object[] {arg});
                            return;
                        } catch (IllegalArgumentException e) {
                            throw new Error("IllegalArgumentException - this should not happen.");
                        }
                    }
                }
            }
        }
        throw new JseIntrospectionException("Cannot find set or add method for property: " + name);
    }

    private static Object toArg(String value, Class type, String sType)
    throws StringCoerceException {
        Object arg = null;
        String toType = null;
        try {
            if (type == String.class && (null == sType || sType.equals("String"))) {
                toType = "String";
                arg = value;
            } else if (type == Character.class  && (null == sType || sType.equals("Character"))) {
                toType = "Character";
                arg = new Character(value.charAt(0));
            } else if (type == Character.TYPE   && (null == sType || sType.equals("char"))) {
                toType = "char";
                arg = new Character(value.charAt(0));
            } else if (type == Short.class  && (null == sType || sType.equals("Short"))) {
                toType = "Short";
                arg = new Short(value);
            } else if (type == Short.TYPE   && (null == sType || sType.equals("short"))) {
                toType = "short";
                arg = new Short(value);
            } else if (type == Byte.class  && (null == sType || sType.equals("Byte"))) {
                toType = "Byte";
                arg = new Byte(value);
            } else if (type == Byte.TYPE   && (null == sType || sType.equals("byte"))) {
                toType = "byte";
                arg = new Byte(value);
            } else if (type == Integer.class  && (null == sType || sType.equals("Integer"))) {
                toType = "Integer";
                arg = new Integer(value);
            } else if (type == Integer.TYPE   && (null == sType || sType.equals("int"))) {
                toType = "int";
                arg = new Integer(value);
            } else if (type == Long.class  && (null == sType || sType.equals("Long"))) {
                toType = "Long";
                arg = new Long(value);
            } else if (type == Long.TYPE   && (null == sType || sType.equals("long"))) {
                toType = "long";
                arg = new Long(value);
            } else if (type == Float.class  && (null == sType || sType.equals("Float"))) {
                toType = "Float";
                arg = new Float(value);
            } else if (type == Float.TYPE   && (null == sType || sType.equals("float"))) {
                toType = "float";
                arg = new Float(value);
            } else if (type == Double.class  && (null == sType || sType.equals("Double"))) {
                toType = "Double";
                arg = new Double(value);
            } else if (type == Double.TYPE   && (null == sType || sType.equals("double"))) {
                toType = "double";
                arg = new Double(value);
            } else if (type == Boolean.class  && (null == sType || sType.equals("Boolean"))) {
                toType = "Boolean";
                arg = new Boolean("true".equals(value.trim()));
            } else if (type == Boolean.TYPE   && (null == sType || sType.equals("boolean"))) {
                toType = "boolean";
                arg = new Boolean("true".equals(value.trim()));
            }
        } catch (Throwable t) {
            throw new StringCoerceException(value, toType, t);
        }
        return arg;
    }

    protected static Object getInstance(String className)
    throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // use reflection to get job instance
        Object obj;
        //try {
            Class objClass = Class.forName(className);
            obj = objClass.newInstance();
            /*
        } catch (ClassNotFoundException e) {
            throw new JseException("Class not found: " + className);
        } catch (InstantiationException e) {
            throw new JseException(
                    "InstantiationException trying no-arg constructor for "
                    + className
                    + ": " + e.getMessage());
        } catch (IllegalAccessException e) {
            throw new JseException(
                    "IllegalAccessException trying no-arg constructor for "
                    + className
                    + ": " + e.getMessage());
        }
        */
        return obj;
    }

}

