import java.util.Arrays;
import java.util.ArrayList;

public class MaxFeeTxHandler {
  private UTXOPool utxoPool;
  
  public MaxFeeTxHandler(UTXOPool utxoPool) {
    this.utxoPool = new UTXOPool(utxoPool);
  }

  public Transaction[] handleTxs(Transaction[] possibleTxs) {
    return simpleHandleTxs(sortTxsByFee(possibleTxs));
  }

  private Transaction[] sortTxsByFee(Transaction[] transactions) {
    Arrays.sort(transactions, (tx1, tx2) -> Double.compare(getTxFee(tx1), getTxFee(tx2)));
    return transactions;
  }

  private Double getTxFee(Transaction tx) {
    if (!isValidTx(tx)) {
      return 0.0;
    }
    
    double inputSum = tx.getInputs().stream()
                        .map(input -> new UTXO(input.prevTxHash, input.outputIndex))
                        .map(utxo -> getUTXOPool().getTxOutput(utxo))
                        .mapToDouble(txOutput -> txOutput.value)
                        .sum();

    double outputSum = tx.getOutputs().stream()
                        .mapToDouble(output -> output.value)
                        .sum();

    return inputSum - outputSum;
  }


  // TxHandler methods copied here for submission compiler

  public UTXOPool getUTXOPool() {
    return this.utxoPool;
  }

  public boolean isValidTx(Transaction tx) {
    ArrayList<UTXO> claimedUTXO = new ArrayList<UTXO>();
    double inputSum = 0;
    double outputSum = 0;

    for (int i = 0; i < tx.numInputs(); i++) {
      Transaction.Input input = tx.getInput(i);

      UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
      if (!this.utxoPool.contains(utxo)) {
        return false;
      }

      Transaction.Output output = this.utxoPool.getTxOutput(utxo);
      if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)){
        return false;
      }

      if (claimedUTXO.contains(utxo)) {
        return false;
      }
      claimedUTXO.add(utxo);
      inputSum += output.value;
    }

    for (int i = 0; i < tx.numOutputs(); i++) {
      Transaction.Output output = tx.getOutput(i);
      if (output.value < 0) {
        return false;
      }
      outputSum += output.value;
    }

    if (inputSum < outputSum) {
      return false;
    }

    return true;
  }


  public Transaction[] simpleHandleTxs(Transaction[] possibleTxs) {
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
