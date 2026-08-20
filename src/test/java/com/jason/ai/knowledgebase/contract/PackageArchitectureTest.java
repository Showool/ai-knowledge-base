package com.jason.ai.knowledgebase.contract;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

class PackageArchitectureTest {

    private static final String BASE_PACKAGE = "com.jason.ai.knowledgebase";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

    @Test
    void controller仅依赖接口模型和业务服务() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + ".repository..", BASE_PACKAGE + ".model.entity..", BASE_PACKAGE + ".model.internal..")
                .check(classes);
    }

    @Test
    void repository不得反向依赖上层职责() {
        noClasses().that().resideInAPackage("..repository..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + ".controller..", BASE_PACKAGE + ".service..", BASE_PACKAGE + ".model.request..",
                        BASE_PACKAGE + ".model.response..")
                .check(classes);
    }

    @Test
    void model不得依赖控制器服务和数据访问层() {
        noClasses().that().resideInAPackage("..model..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + ".controller..", BASE_PACKAGE + ".service..", BASE_PACKAGE + ".repository..",
                        BASE_PACKAGE + ".config..", BASE_PACKAGE + ".security..")
                .check(classes);
    }

    @Test
    void 请求和响应模型不得相互依赖() {
        noClasses().that().resideInAPackage(BASE_PACKAGE + ".model.request..")
                .should().dependOnClassesThat().resideInAPackage(BASE_PACKAGE + ".model.response..")
                .check(classes);
        noClasses().that().resideInAPackage(BASE_PACKAGE + ".model.response..")
                .should().dependOnClassesThat().resideInAPackage(BASE_PACKAGE + ".model.request..")
                .check(classes);
    }

    @Test
    void common不得反向依赖应用职责层() {
        noClasses().that().resideInAPackage("..common..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE_PACKAGE + ".controller..", BASE_PACKAGE + ".service..", BASE_PACKAGE + ".repository..",
                        BASE_PACKAGE + ".model..", BASE_PACKAGE + ".security..")
                .check(classes);
    }

    @Test
    void 顶层职责包不得形成循环依赖() {
        slices().matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }
}