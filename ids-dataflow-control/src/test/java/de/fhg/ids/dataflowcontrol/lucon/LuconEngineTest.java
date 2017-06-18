package de.fhg.ids.dataflowcontrol.lucon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import de.fhg.aisec.ids.api.policy.DecisionRequest;
import de.fhg.aisec.ids.api.policy.PolicyDecision;
import de.fhg.aisec.ids.api.policy.PolicyDecision.Decision;
import de.fhg.ids.dataflowcontrol.PolicyDecisionPoint;

public class LuconEngineTest {
	// Solving Towers of Hanoi in only two lines. Prolog FTW!
	private final static String HANOI_THEORY = 	"move(1,X,Y,_) :-  write('Move top disk from '), write(X), write(' to '), write(Y), nl. \n" +
											"move(N,X,Y,Z) :- N>1, M is N-1, move(M,X,Z,Y), move(1,X,Y,_), move(M,Z,Y,X). ";

	private final static String EXAMPLE_POLICY = 
			"/* **********************************************************\n" + 
			"    Prolog representation of a data flow policy\n" + 
			"    \n" + 
			"    Source: test2\n" + 
			" 	\n" + 
			" 	Do not edit this file, it has been generated automatically\n" + 
			" 	by XText/Xtend.\n" + 
			"   ********************************************************** */\n" + 
			"				\n" + 
			"% Allow the following predicates to be scattered around the prolog file.\n" + 
			"% Otherwise Prolog will issue a warning if they are not stated in subsequent lines.		\n" + 
			"%%%%%%%% Rules %%%%%%%%%%%%\n" + 
			"rule(deleteAfterOneMonth).\n" + 
			"has_target(deleteAfterOneMonth, hadoopCluster).\n" + 
			"receives_label(deleteAfterOneMonth,private).\n" + 
			"has_obligation(deleteAfterOneMonth, ObligationImpl_19e90d3f).\n" + 
			"requires_prerequisites(ObligationImpl_19e90d3f, (delete_after_days(X),X<30)).\n" + 
			"requires_action(ObligationImpl_19e90d3f, drop).\n" + 
			"\n" + 
			"%%%%% Services %%%%%%%%%%%%\n" + 
			"	service(anonymizer).\n" + 
			"		has_property(anonymizer,myProp).\n" + 
			"	service(hadoopCluster).\n" + 
			"		has_capability(hadoopCluster,deletion).\n" + 
			"		has_property(hadoopCluster,anonymizes).";
	
	
	@Test
	public void testLoadingTheoryGood() throws InvalidTheoryException, IOException {
		LuconEngine e = new LuconEngine(null);
		e.loadPolicy(new ByteArrayInputStream(HANOI_THEORY.getBytes()));
		String json = e.getTheoryAsJSON();
		assertTrue(json.startsWith("{\"theory\":\"move(1,X,Y,"));
		String prolog = e.getTheory();
		assertTrue(prolog.trim().startsWith("move(1,X,Y"));
	}

	@Test
	public void testLoadingTheoryNotGood() throws InvalidTheoryException, IOException {
		LuconEngine e = new LuconEngine(System.out);
		try {
			e.loadPolicy(new ByteArrayInputStream("This is invalid".getBytes()));
		} catch (InvalidTheoryException ex) {
			return;	// Expected
		}
		fail("Could load invalid theory without exception");
	}
	
	@Test
	public void testSolve1() throws InvalidTheoryException, IOException, NoMoreSolutionException {
		LuconEngine e = new LuconEngine(System.out);
		e.loadPolicy(new ByteArrayInputStream(HANOI_THEORY.getBytes()));
		try {
			List<SolveInfo> solutions = e.query("move(3,left,right,center). ", true);
			assertTrue(solutions.size()==1);
			for (SolveInfo solution : solutions) {
				System.out.println(solution.getSolution().toString());
				System.out.println(solution.hasOpenAlternatives());
				
				System.out.println(solution.isSuccess());
			}
		} catch (MalformedGoalException | NoSolutionException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}
	
	@Test
	public void testSolve2() throws InvalidTheoryException, IOException, NoMoreSolutionException {
		LuconEngine e = new LuconEngine(System.out);
		e.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		try {
			List<SolveInfo> solutions = e.query("rule(X), has_target(X, T).", true);
			assertTrue(solutions.size()==1);
			for (SolveInfo solution : solutions) {
				System.out.println(solution.getSolution().toString());
				System.out.println(solution.hasOpenAlternatives());
				
				System.out.println(solution.isSuccess());
			}
		} catch (MalformedGoalException | NoSolutionException e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}
	
	@Test
	public void testPolicyDecision() {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		
		Map<String, String> attributes = new HashMap<>();
		attributes.put("some_message_key", "some_message_value");
		PolicyDecision dec = pdp.requestDecision(new DecisionRequest("seda:test_source", "ids://some_endpoint", attributes, System.getenv()));
		assertEquals(Decision.ALLOW, dec.getDecision());
	}

	@Test
	public void testListRules() {
		PolicyDecisionPoint pdp = new PolicyDecisionPoint();
		pdp.activate(null);
		
		// Without any policy, we expect an empty list of rules
		List<String> emptyList = pdp.listRules();
		assertNotNull(emptyList);
		assertTrue(emptyList.isEmpty());
				
		// Load a policy
		pdp.loadPolicy(new ByteArrayInputStream(EXAMPLE_POLICY.getBytes()));
		
		// We now expect 1 rule
		List<String> oneList = pdp.listRules();
		assertNotNull(oneList);
		assertEquals(1, oneList.size());
		assertEquals("deleteAfterOneMonth", oneList.get(0));
	}
}
