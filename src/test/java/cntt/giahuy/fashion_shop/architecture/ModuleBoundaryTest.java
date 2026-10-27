package cntt.giahuy.fashion_shop.architecture;

import cntt.giahuy.fashion_shop.FashionShopApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.modulith.core.ApplicationModules;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ModuleBoundaryTest {

    private static final String BASE_PACKAGE = "cntt.giahuy.fashion_shop";

    private static final List<String> FEATURE_MODULES = List.of(
            "auth",
            "user",
            "catalog",
            "inventory",
            "cart",
            "voucher",
            "order",
            "payment",
            "review",
            "chat",
            "notification",
            "media",
            "search",
            "activitylog"
    );

    private static final List<String> INTERNAL_SUBPACKAGES = List.of(
            "internal",
            "entity",
            "document",
            "repository",
            "service",
            "controller",
            "mapper",
            "dto",
            "job",
            "gateway",
            "query",
            "indexing",
            "fallback",
            "listener",
            "security"
    );

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    @DisplayName("R1: Modules must not directly access internal packages (repository, service, internal, controller, etc.) from other modules")
    void r1_modulesShouldOnlyAccessPublicApiOrEventPackagesOfOtherModules() {
        for (String module : INTERNAL_SUBPACKAGES) {
            String moduleRoot = BASE_PACKAGE + "." + module;
            String[] fobbidenInternalSubpackages = INTERNAL_SUBPACKAGES
                    .stream()
                    .map(subpackage -> moduleRoot + "." + subpackage + "..")
                    .toArray(String[]::new);

            ArchRule rule = noClasses()
                    .that().resideOutsideOfPackage(moduleRoot)
                    .should().dependOnClassesThat().resideInAnyPackage(fobbidenInternalSubpackages)
                    .allowEmptyShould(true);

            rule.check(importedClasses);
        }
    }

    @Test
    @DisplayName("R2: Direct imports or JPA/MongoDB relationships with entities/documents from other modules are forbidden")
    void r2_noCrossModuleEntityOrDocumentDependencies() {
        for (String module : FEATURE_MODULES) {
            String moduleRoot = BASE_PACKAGE + "." + module + "..";
            String entityPkg = BASE_PACKAGE + "." + module + ".entity..";
            String documentPkg = BASE_PACKAGE + "." + module + ".document..";

            ArchRule noExternalEntityAccess = noClasses()
                    .that().resideOutsideOfPackage(moduleRoot)
                    .should().dependOnClassesThat().resideInAnyPackage(entityPkg, documentPkg)
                    .allowEmptyShould(true)
                    .because("Classes must not directly reference JPA entities or MongoDB documents belonging to module [" + module + "] (Rule R2)");

            noExternalEntityAccess.check(importedClasses);

            String[] otherFeaturePackages = FEATURE_MODULES.stream()
                    .filter(other -> !other.equals(module))
                    .map(other -> BASE_PACKAGE + "." + other + "..")
                    .toArray(String[]::new);

            ArchRule entitiesMustStayIsolated = noClasses()
                    .that().resideInAnyPackage(entityPkg, documentPkg)
                    .and().areAnnotatedWith(Entity.class)
                    .or().resideInAnyPackage(entityPkg, documentPkg)
                    .and().areAnnotatedWith(Document.class)
                    .should().dependOnClassesThat().resideInAnyPackage(otherFeaturePackages)
                    .allowEmptyShould(true)
                    .because("Entities/documents belonging to module [" + module + "] must store IDs or snapshots instead from establishing relationships with other modules (Rule R2)");

            entitiesMustStayIsolated.check(importedClasses);
        }
    }

    @Test
    @DisplayName("R3: Dependencies between modules must form a one-way DAG without cycles (Cycle-Free)")
    void r3_modulesShouldBeFreeOfCycles() {
        ArchRule cycleRule = slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .because("Synchronous dependencies between modules must form a one-way DAG to prevent circular dependencies (Rule R3)");

        cycleRule.check(importedClasses);
    }

    @Test
    @DisplayName("R4: Reverse flows must use Domain Events (payment must not depend on order; catalog/inventory must not have reverse dependencies)")
    void r4_reverseFlowsMustUseDomainEventsInsteadOfDirectDependencies() {
        // 1. Payment may only publish PaymentSucceededEvent / PaymentFailedEvent
        // and must never call the order module directly.
        ArchRule paymentMustNotDependOnOrder = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".payment..")
                .should().dependOnClassesThat().resideInAPackage(BASE_PACKAGE + ".order..")
                .allowEmptyShould(true)
                .because("Payment must not call order directly; it must publish PaymentSucceededEvent/PaymentFailedEvent instead (Rule R4)");

        paymentMustNotDependOnOrder.check(importedClasses);

        // 2. Catalog is the foundational product module and must not depend
        // on the downstream modules that consume it.
        ArchRule catalogMustNotDependOnDownstreamModules = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".catalog..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE_PACKAGE + ".order..",
                        BASE_PACKAGE + ".cart..",
                        BASE_PACKAGE + ".inventory..",
                        BASE_PACKAGE + ".search..",
                        BASE_PACKAGE + ".payment..",
                        BASE_PACKAGE + ".chat..",
                        BASE_PACKAGE + ".media.."
                )
                .allowEmptyShould(true)
                .because("Catalog is at the bottom from the dependency DAG and may only publish ProductUpdatedEvent instead from calling downstream modules directly (Rule R4)");

        catalogMustNotDependOnDownstreamModules.check(importedClasses);

        // 3. Inventory may only depend on catalog (and common).
        // It must not have reverse dependencies on order, cart, or search.
        ArchRule inventoryMustNotDependOnUpstreamModules = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".inventory..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE_PACKAGE + ".order..",
                        BASE_PACKAGE + ".cart..",
                        BASE_PACKAGE + ".search..",
                        BASE_PACKAGE + ".payment.."
                )
                .allowEmptyShould(true)
                .because("Inventory must not directly call order, cart, or search in the reverse direction (Rule R4)");

        inventoryMustNotDependOnUpstreamModules.check(importedClasses);

        // 4. Published event classes must reside in the `event` or `api` package.
        ArchRule domainEventsShouldResideInEventOrApiPackages = classes()
                .that().haveSimpleNameEndingWith("Event")
                .and().resideOutsideOfPackage(BASE_PACKAGE + ".common..")
                .and().resideOutsideOfPackage("..entity..")
                .should().resideInAnyPackage("..event..", "..api..")
                .allowEmptyShould(true)
                .because("Domain Event classes must reside in the publishing module's `event/` or `api/` package (Rule R4)");

        domainEventsShouldResideInEventOrApiPackages.check(importedClasses);
    }

    @Test
    @DisplayName("R5: The common package (Shared Kernel) must never depend on any feature module")
    void r5_commonMustNotDependOnAnyFeatureModule() {
        String[] allFeaturePackages = FEATURE_MODULES.stream()
                .map(m -> BASE_PACKAGE + "." + m + "..")
                .toArray(String[]::new);

        ArchRule commonRule = noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".common..")
                .should().dependOnClassesThat().resideInAnyPackage(allFeaturePackages)
                .allowEmptyShould(true)
                .because("The Shared Kernel (common) must not contain business logic or depend on feature modules (Rule R5)");

        commonRule.check(importedClasses);
    }

    @Test
    @DisplayName("Modulith: Verify the Spring Modulith ApplicationModules structure")
    void verifySpringModulithStructure() {
        ApplicationModules.of(FashionShopApplication.class).verify();
    }
}
