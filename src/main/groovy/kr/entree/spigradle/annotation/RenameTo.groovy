package kr.entree.spigradle.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Created by JunHyung Lim on 2020-02-05
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@interface RenameTo {
    String value()
}