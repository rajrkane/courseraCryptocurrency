import java.util.ArrayList;

public class TxHandler {
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    public UTXOPool getUTXOPool() {
      return this.utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<UTXO> claimedUTXO = new ArrayList<UTXO>();
        double inputSum = 0;
        double outputSum = 0;

        for (int i = 0; i < tx.numInputs(); i++) {
          Transaction.Input input = tx.getInput(i);

          // Is the claimed output in the current UTXO pool?
          UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
          if (!this.utxoPool.contains(utxo)) {
            return false;
          }

          // Are all the signatures valid?
          Transaction.Output output = this.utxoPool.getTxOutput(utxo);
          if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)){
            return false;
          }

          // Is every UTXO claimed only once?
          if (claimedUTXO.contains(utxo)) {
            return false;
          }
          claimedUTXO.add(utxo);
          inputSum += output.value;
        }

        // Are all of the output values non-negative?
        for (int i = 0; i < tx.numOutputs(); i++) {
          Transaction.Output output = tx.getOutput(i);
          if (output.value < 0) {
            return false;
          }
          outputSum += output.value;
        }

        // Is the sum of input values greater than or equal to the sum of output values?
        if (inputSum < outputSum) {
          return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();
        
        for (Transaction tx : possibleTxs) {
          if (isValidTx(tx)) {
            acceptedTxs.add(tx);
            
            // Drain current UTXO pool
            for (Transaction.Input input : tx.getInputs()) {
              UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
              this.utxoPool.removeUTXO(utxo);
            }

            // Add created coins to pool
            for (int i = 0; i < tx.numOutputs(); i++) {
              UTXO utxo = new UTXO(tx.getHash(), i);
              this.utxoPool.addUTXO(utxo, tx.getOutput(i));
            }
          }
        }

        Transaction[] acceptedTransactions = new Transaction[acceptedTxs.size()];
        acceptedTxs.toArray(acceptedTransactions);
        return acceptedTransactions;
    }

}
