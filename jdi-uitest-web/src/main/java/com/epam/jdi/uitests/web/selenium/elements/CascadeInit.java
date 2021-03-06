/*
 * Copyright 2004-2016 EPAM Systems
 *
 * This file is part of JDI project.
 *
 * JDI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JDI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty ofMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JDI. If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.jdi.uitests.web.selenium.elements;

import com.epam.commons.LinqUtils;
import com.epam.commons.pairs.Pairs;
import com.epam.jdi.uitests.core.interfaces.base.IBaseElement;
import com.epam.jdi.uitests.core.interfaces.base.IComposite;
import com.epam.jdi.uitests.web.selenium.elements.apiInteract.ContextType;
import com.epam.jdi.uitests.web.selenium.elements.composite.Site;
import com.epam.jdi.uitests.web.selenium.elements.composite.WebPage;
import com.epam.jdi.uitests.web.selenium.elements.pageobjects.annotations.Frame;
import com.epam.jdi.uitests.web.selenium.elements.pageobjects.annotations.JFindBy;
import com.epam.jdi.uitests.web.selenium.elements.pageobjects.annotations.JPage;
import com.epam.jdi.uitests.web.selenium.elements.pageobjects.annotations.WebAnnotationsUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.epam.commons.LinqUtils.foreach;
import static com.epam.commons.ReflectionUtils.*;
import static com.epam.commons.StringUtils.LINE_BREAK;
import static com.epam.commons.TryCatchUtil.tryGetResult;
import static com.epam.jdi.uitests.core.settings.JDIData.APP_VERSION;
import static com.epam.jdi.uitests.core.settings.JDISettings.exception;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.isStatic;

/**
 * Created by Roman_Iovlev on 6/10/2015.
 */
public abstract class CascadeInit implements IBaseElement {

    public synchronized static void InitElements(Object parent, String driverName) {
        if (parent.getClass().getName().contains("$")) return;
        Class<?> parentType = parent.getClass();

        initSubElements(parent, driverName);

        if (isClass(parentType, WebPage.class) && parentType.isAnnotationPresent(JPage.class))
            WebAnnotationsUtil.fillPageFromAnnotaiton((WebPage) parent, parentType.getAnnotation(JPage.class), null);
    }

    private static void initSubElements(Object parent, String driverName) {
        foreach(deepGetFields(parent, IBaseElement.class),
                field -> setElement(parent, field, driverName));
    }

    private static List<Field> deepGetFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        if (isInterface(clazz, IBaseElement.class))
            result.addAll(deepGetFields(clazz.getSuperclass()));
        result.addAll(Arrays.asList(clazz.getDeclaredFields()));
        return result;
    }

    private static List<Field> deepGetFields(Object obj, Class<?> type) {
        return LinqUtils.where(deepGetFields(obj.getClass()), field -> !isStatic(field.getModifiers()) && (isClass(field, type) || isInterface(field, type)));
    }
    public synchronized static void initStaticPages(Class<?> parentType, String driverName) {
        foreach(getStaticFields(parentType, BaseElement.class),
                field -> setElement(parentType, field, driverName));
    }

    public synchronized static <T extends Site> T initPages(Class<T>  site, String driverName) {
        T instance = tryGetResult(site::newInstance);
        instance.setDriverName(driverName);
        InitElements(instance, driverName);
        return instance;
    }

    private static void setElement(Class<?> parentType, Field field, String driverName) {
        try {
            Class<?> type = field.getType();
            BaseElement instance;
            if (isClass(type, WebPage.class)) {
                instance = (BaseElement) getValueField(field, null);
                if (instance == null)
                    instance = (BaseElement) type.newInstance();
                fillPage(instance, field, parentType);
            } else {
                instance = createChildFromFieldStatic(parentType, field, type, driverName);
                instance.function = WebAnnotationsUtil.getFunction(field);
            }
            instance.setName(field);
            if (instance.getClass().getSimpleName().equals(""))
                instance.setTypeName(type.getSimpleName());
            instance.setParentName(parentType.getClass().getSimpleName());
            field.set(null, instance);
            if (isInterface(field, IComposite.class))
                InitElements(instance, driverName);
        } catch (Exception ex) {
            throw exception("Error in setElement for field '%s' with parent '%s'", field.getName(), parentType.getClass().getSimpleName() + LINE_BREAK + ex.getMessage());
        }
    }

    private static String getClassName(Object obj) {
        return obj == null ? "NULL Class" : obj.getClass().getSimpleName();
    }

    private static void setElement(Object parent, Field field, String driverName) {
        try {
            Class<?> type = field.getType();
            BaseElement instance;
            if (isClass(type, WebPage.class)) {
                instance = (BaseElement) getValueField(field, parent);
                if (instance == null)
                    instance = (BaseElement) type.newInstance();
                fillPage(instance, field, parent != null ? parent.getClass() : null);
            } else {
                instance = createChildFromField(parent, field, type, driverName);
                instance.function = WebAnnotationsUtil.getFunction(field);
            }
            instance.setName(field);
            instance.avatar.setDriverName(driverName);
            if (instance.getClass().getSimpleName().equals(""))
                instance.setTypeName(type.getSimpleName());
            instance.setParentName(getClassName(parent));
            field.set(parent, instance);
            if (isInterface(field, IComposite.class))
                InitElements(instance, driverName);
        } catch (Exception ex) {
            throw exception("Error in setElement for field '%s' with parent '%s'", field.getName(),
                    getClassName(parent) + LINE_BREAK + ex.getMessage());
        }
    }

    private static void fillPage(BaseElement instance, Field field, Class<?> parentType) {
        if (field.isAnnotationPresent(JPage.class))
            WebAnnotationsUtil.fillPageFromAnnotaiton((WebPage) instance, field.getAnnotation(JPage.class), parentType);
    }

    private static BaseElement createChildFromFieldStatic(Class<?> parentClass, Field field, Class<?> type, String driverName) {
        BaseElement instance = (BaseElement) getValueField(field, null);
        if (instance == null)
            try {
                instance = getElementInstance(type, field.getName(), getNewLocator(field), driverName);
            } catch (Exception ex) {
                throw exception(format("Can't create child for parent '%s' with type '%s'",
                        parentClass.getSimpleName(), field.getType().getSimpleName()));
            }
        else if (instance.getLocator() == null)
            instance.avatar.byLocator = getNewLocator(field);
        instance.avatar.context = new Pairs<>();
        By frameBy = WebAnnotationsUtil.getFrame(field.getDeclaredAnnotation(Frame.class));
        if (frameBy != null)
            instance.avatar.context.add(ContextType.Frame, frameBy);
        return instance;
    }

    private static BaseElement createChildFromField(Object parent, Field field, Class<?> type, String driverName) {
        BaseElement instance = (BaseElement) getValueField(field, parent);
        if (instance == null)
            try {
                instance = getElementInstance(type, field.getName(), getNewLocator(field), driverName);
            } catch (Exception ex) {
                throw exception(
                        format("Can't create child for parent '%s' with type '%s'",
                                parent.getClass().getSimpleName(), field.getType().getSimpleName()));
            }
        else if (instance.getLocator() == null)
            instance.avatar.byLocator = getNewLocator(field);
        instance.avatar.context = (isBaseElement(parent))
                ? ((BaseElement) parent).avatar.context.copy()
                : new Pairs<>();
        if (type != null) {
            By frameBy = WebAnnotationsUtil.getFrame(type.getDeclaredAnnotation(Frame.class));
            if (frameBy != null)
                instance.avatar.context.add(ContextType.Frame, frameBy);
        }
        if (isBaseElement(parent)) {
            By parentLocator = ((BaseElement) parent).getLocator();
            if (parentLocator != null)
                instance.avatar.context.add(ContextType.Locator, parentLocator);
        }
        return instance;
    }

    private static boolean isBaseElement(Object obj) {
        return isClass(obj.getClass(), BaseElement.class);
    }

    private static BaseElement getElementInstance(Class<?> type, String fieldName, By newLocator, String driverName) {
        try {
            if (!type.isInterface()) {
                BaseElement instance = (BaseElement) type.newInstance();
                instance.avatar.byLocator = newLocator;
                instance.avatar.setDriverName(driverName);
                return instance;
            }
            Class classType = MapInterfaceToElement.getClassFromInterface(type);
            if (classType != null)
                return (BaseElement) classType.getDeclaredConstructor(By.class).newInstance(newLocator);
            throw exception("Unknown interface: " + type +
                    ". Add relation interface -> class in VIElement.InterfaceTypeMap");
        } catch (Exception ex) {
            throw exception("Error in getElementInstance for field '%s' with type '%s'", fieldName, type.getSimpleName() +
                    LINE_BREAK + ex.getMessage());
        }
    }

    private static By getNewLocator(Field field) {
        try {
            By byLocator = null;
            String locatorGroup = APP_VERSION;
            if (locatorGroup != null) {
                JFindBy jFindBy = field.getAnnotation(JFindBy.class);
                if (jFindBy != null && locatorGroup.equals(jFindBy.group()))
                    byLocator = WebAnnotationsUtil.getFindByLocator(jFindBy);
            }
            return (byLocator != null)
                    ? byLocator
                    : WebAnnotationsUtil.getFindByLocator(field.getAnnotation(FindBy.class));
        } catch (Exception ex) {
            throw exception("Error in get locator for type '%s'", field.getType().getName() +
                    LINE_BREAK + ex.getMessage());
        }
    }

}