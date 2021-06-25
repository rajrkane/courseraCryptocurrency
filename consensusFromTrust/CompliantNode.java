import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// Score: 89/100 (passed)

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private double p_graph, p_malicious, p_txDistribution;
    private int numRounds, round, prevRound;
    private boolean[] followees, malicious;
    private Set<Transaction> pendingTransactions, consensusTransactions;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        this.malicious = new boolean[followees.length];
        Arrays.fill(this.malicious, Boolean.FALSE);
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;
        this.consensusTransactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        if (this.round == numRounds) {
          return this.consensusTransactions;
        }
        this.prevRound = this.round;
        return this.pendingTransactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        this.round++;
        if (this.round > this.prevRound) {
          this.pendingTransactions.clear();
        }
        Set<Integer> senders = candidates.stream().map(c -> c.sender).collect(Collectors.toSet());
        for (int i = 0; i < this.followees.length; i++) {
          if (this.followees[i] && !senders.contains(i)) {
            this.malicious[i] = true;
          }
        }
        for (Candidate c : candidates) {
          if (!this.consensusTransactions.contains(c.tx) && !this.malicious[c.sender]) {
            this.consensusTransactions.add(c.tx);
            this.pendingTransactions.add(c.tx);
          }
        }
    }
}
