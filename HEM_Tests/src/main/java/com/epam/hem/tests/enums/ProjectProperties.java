package com.epam.hem.tests.enums;

/**
 * Created by Pavel_Shcherbakov1 on 2015-10-09
 */
public enum ProjectProperties {
    Name(0), CreationDate(1), CreationUser(2), OwnerUser(3);

    public int value;
    ProjectProperties(int value) { this.value = value; }
}
