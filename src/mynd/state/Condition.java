package mynd.state;

public interface Condition
{

	/**
	 * Tests if this condition is satisfied by the given state.
	 * 
	 * @param state
	 * @return true iff this condition is satisfied by the given state.
	 */
	boolean isSatisfiedIn(State state);

	/**
	 * Dump this condition. Only for debugging.
	 */
	void dump();

	// ------------------------Modified for LAO*------------------------------
	/** 
	 * @Title: isSatisfiedInForLAOStar 
	 * @Description: TODO
	 * @param @param state
	 * @param @return 
	 * @return boolean
	 * @throws 
	 */ 
	boolean isSatisfiedInForLAOStar(State state);
	
	// --------------------------End------------------------------------------------
}
