// THOMAS NGO
// tngo0508@csu.fullerton.edu
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
	public static final int CUT_OFF_AGE = 10;
	private TransactionPool _txnPool = new TransactionPool();
	private BlockNode _blockChain;

	// all information required in handling a block in block chain
	private class BlockNode {
		public Block b;
		public BlockNode parent;
		public ArrayList<BlockNode> children;
		public int height;
		// utxo pool for making a new block on top of this block
		private UTXOPool uPool;

		public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
			this.b = b;
			this.parent = parent;
			children = new ArrayList<BlockNode>();
			this.uPool = uPool;
			if (parent != null) {
				height = parent.height + 1;
				parent.children.add(this);
			} else {
				height = 1;
			}
		}

		public UTXOPool getUTXOPoolCopy() {
			return new UTXOPool(uPool);
		}
	}

	/*
	 * create an empty block chain with just a genesis block. Assume genesis block
	 * is a valid block
	 */
	public BlockChain(Block genesisBlock) {
		// IMPLEMENT THIS

		// process the coinbase transaction which is the first transaction in the block.
		// This is where
		// miner collects the block reward or fees

		// prepare the UTXOPool for the block and the global Transaction Pool
		UTXOPool uPool = new UTXOPool();
		TransactionPool txnPool = new TransactionPool();

		// add the coinbase transaction into the global transaction pool
		Transaction coinBaseTx = genesisBlock.getCoinbase();
		txnPool.addTransaction(coinBaseTx);

		int numOpOfCoinBase = coinBaseTx.numOutputs();
		for (int i = 0; i < numOpOfCoinBase; i++) {
			// get the transaction hash
			byte[] coinBaseTxHash = coinBaseTx.getHash();

			// create UTXO
			UTXO utxo = new UTXO(coinBaseTxHash, i);

			// get the coinbase transaction output
			Transaction.Output txOut = coinBaseTx.getOutput(i);

			// add the new utxo into UXTOPool
			uPool.addUTXO(utxo, txOut);
		}

		// process the rest of block
		ArrayList<Transaction> txns = genesisBlock.getTransactions();
		for (Transaction tx : txns) {
			if (tx != null) {
				// keep track the transaction output history from genesisBlock
				int numOpOfCurrTX = tx.numOutputs();
				for (int i = 0; i < numOpOfCurrTX; i++) {
					// get the transaction hash
					byte[] txHash = tx.getHash();

					// create UTXO
					UTXO utxo = new UTXO(txHash, i);

					// get the transaction output
					Transaction.Output txOut = tx.getOutput(i);

					// add unspent coin to the UTXOPool
					uPool.addUTXO(utxo, txOut);
				}
				// add the transaction into the global transaction pool
				txnPool.addTransaction(tx);
			}
		}

		_blockChain = new BlockNode(genesisBlock, null, uPool);
	}

	// helper function - find the BlockNode that holds the max height
	public BlockNode getMaxheighBlockNode() {		
		int maxHeight = _blockChain.height;
		BlockNode re = _blockChain;
		for (BlockNode b: _blockChain.children) {
			if (maxHeight < b.height) {
				maxHeight = b.height;
				re = b;
			}
		}
		
		return re;
	}

	/*
	 * Get the maximum height block
	 */
	public Block getMaxHeightBlock() {
		// IMPLEMENT THIS
//		System.out.println("maxHeight Block hash: " + getMaxheighBlockNode().b.getHash());
		return getMaxheighBlockNode().b;
	}

	/*
	 * Get the UTXOPool for mining a new block on top of max height block
	 */
	public UTXOPool getMaxHeightUTXOPool() {
		// IMPLEMENT THIS
		return getMaxheighBlockNode().uPool;
	}

	/*
	 * Get the transaction pool to mine a new block
	 */
	public TransactionPool getTransactionPool() {
		// IMPLEMENT THIS
		return _txnPool;
	}

	// helper function for addBlock()
	public BlockNode getParent(byte[] bHash) {
		ByteArrayWrapper bArr1 = new ByteArrayWrapper(bHash);
		Stack<BlockNode> s = new Stack<BlockNode>();
		s.add(_blockChain);
		Set<BlockNode> visited = new HashSet<BlockNode>();
		while (!s.isEmpty()) {
			BlockNode curr = s.pop();
			byte[] currBlockHash = curr.b.getHash();
			ByteArrayWrapper bArr2 = new ByteArrayWrapper(currBlockHash);
			if (bArr1.equals(bArr2))
				return curr;
			if (!curr.children.isEmpty()) {
				for (BlockNode b : curr.children) {
					if (!visited.contains(b)) {
						s.add(b);
						visited.add(b);
					}
				}
			}
		}

		return null;
	}

	/*
	 * Add a block to block chain if it is valid. For validity, all transactions
	 * should be valid and block should be at height > (maxHeight - CUT_OFF_AGE).
	 * For example, you can try creating a new block over genesis block (block
	 * height 2) if blockChain height is <= CUT_OFF_AGE + 1. As soon as height >
	 * CUT_OFF_AGE + 1, you cannot create a new block at height 2. Return true of
	 * block is successfully added
	 */
	public boolean addBlock(Block b) {
		// IMPLEMENT THIS
		// If you receive a block which claims to be a genesis block (parent is a null
		// hash),
		// return false
		if (b.getPrevBlockHash() == null)
			return false;

		// check if parent is available
		BlockNode parent = getParent(b.getPrevBlockHash());
//	   System.out.println(parent);
		if (parent == null)
			return false;

		// check the block's height
//	   System.out.println(getMaxheighBlockNode().height);
		int pHeight = parent.height;
		int maxHeight = getMaxheighBlockNode().height;
		if (pHeight < maxHeight - CUT_OFF_AGE)
			return false;
		
		if (maxHeight > CUT_OFF_AGE + 1 && pHeight + 1 == 2) {
			System.out.println(maxHeight);
			return false;
		}	

		// check for all transactions' validity from previous or parent block
		ArrayList<Transaction> txns = b.getTransactions();

		UTXOPool uPool = new UTXOPool(parent.getUTXOPoolCopy());
		for (Transaction tx : txns) {
			TxHandler checkTxn = new TxHandler(uPool);
			if (checkTxn.isValidTx(tx) == false)
				return false;

			// Avoid double spends
			// remove the spent coins or spent UTXO from UTXOPool
			for (Transaction.Input in : tx.getInputs()) {
				byte[] txHash = in.prevTxHash;
				int index = in.outputIndex;
				UTXO utxo = new UTXO(txHash, index);
				uPool.removeUTXO(utxo);
			}

			// add the new unspent coins or new UTXO to UTXOPool
			for (int i = 0; i < tx.numOutputs(); i++) {
				UTXO utxo = new UTXO(tx.getHash(), i);
				uPool.addUTXO(utxo, tx.getOutput(i));
			}

			// keep removing transactions from the global transaction pool if a new block is
			// created
			_txnPool.removeTransaction(tx.getHash());
		}

		// after handling the transactions of previous block, update the UTXOPool with
		// the unspent coins of new block's coinbase transaction
		Transaction coinBaseTx = b.getCoinbase();
		int numOpOfCoinBaseTx = coinBaseTx.numOutputs();
		for (int i = 0; i < numOpOfCoinBaseTx; i++) {
			byte[] coinBaseTxHash = coinBaseTx.getHash();
			UTXO utxo = new UTXO(coinBaseTxHash, i);
			uPool.addUTXO(utxo, coinBaseTx.getOutput(i));
		}

		// add new block into the chain
		BlockNode newBlock = new BlockNode(b, parent, uPool);
		_blockChain.children.add(newBlock);
		return true;
	}

	/*
	 * Add a transaction in transaction pool
	 */
	public void addTransaction(Transaction tx) {
		// IMPLEMENT THIS
		_txnPool.addTransaction(tx);
	}
}
