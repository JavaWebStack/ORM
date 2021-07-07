package org.javawebstack.orm.test.querybuilding;

import org.javawebstack.orm.Model;
import org.javawebstack.orm.ORM;
import org.javawebstack.orm.Repo;
import org.javawebstack.orm.exception.ORMConfigurationException;
import org.javawebstack.orm.query.Query;
import org.javawebstack.orm.test.ORMTestCase;
import org.javawebstack.orm.test.shared.models.Datatype;
import org.javawebstack.orm.test.shared.models.EmptyUUIDModel;
import org.javawebstack.orm.test.shared.verification.QueryVerification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.javawebstack.orm.test.shared.setup.ModelSetup.setUpModel;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhereClauseTest extends ORMTestCase {

    private Repo<Datatype> repo;

    @BeforeEach
    void setUp() {
        this.repo = setUpModel(Datatype.class);
    }

    /*
     * Normal Cases
     */

    @Test
    void testImplicitEqualOperation() {
        Query<Datatype> query = repo.where("wrapper_boolean", true);
        performEqualityTest("`wrapper_boolean` = ?",query);
    }

    @Test
    void testImplicitEqualOperationReverse() {
        Query<Datatype> query = repo.where(true, "wrapper_boolean");
        performEqualityTest("? = `wrapper_boolean`", query);
    }

    @Test
    void testImplicitEqualOperationWithWrongColumnName() {
        // Note the missing b
        Query<Datatype> query = repo.where("wrapper_oolean", true);
        performEqualityTest("? = ?", query);
    }

    @Test
    void testExplicitEqualOperation() {
        Query<Datatype> query = repo.where("wrapper_boolean",  "=",true);
        performEqualityTest("`wrapper_boolean` = ?", query);
    }

    @Test
    void testExplicitEqualOperationReverse() {
        Query<Datatype> query = repo.where(true, "=" , "wrapper_boolean");
        performEqualityTest("? = `wrapper_boolean`", query);
    }

    @Test
    void testExplicitEqualOperationWithWrongColumnName() {
        // Note the missing b
        Query<Datatype> query = repo.where("wrapper_oolean", "=", true);
        performEqualityTest("? = ?", query);
    }

    @Test
    void testImplicitEqualOnId() {
        Query<Datatype> query = repo.whereId(3);
        performEqualityTest("`id` = ?", query);
    }

    @Test
    void testImplicitEqualOnUuid() throws ORMConfigurationException {
        ORM.register(EmptyUUIDModel.class, sql());
        Query<EmptyUUIDModel> query = ORM.repo(EmptyUUIDModel.class).whereId("unique-stuff");
        performEqualityTest("`uuid` = ?", query);
    }

    @Test
    void testNonsenseOperation() {
        Query<Datatype> query = repo.where("wrapper_boolean", "NOOPERATION", true);
        performEqualityTest("`wrapper_boolean` NOOPERATION ?", query);
    }



    private void performEqualityTest(String expectedtring, Query<? extends Model> query) {
        new QueryVerification(query).assertSectionEquals(expectedtring);
    }
}
