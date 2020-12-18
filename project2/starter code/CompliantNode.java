import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import static java.util.stream.Collectors.toSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    double p_graph_;
    double p_malicious_;
    double p_txDistribution_;
    int numRounds_;
    boolean[] followees_;
    HashSet<Transaction> pendingTransactions_;
    boolean[] malicious_nodes;
    int currentRound = 0;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        p_graph_ = p_graph;
        p_malicious_ = p_malicious;
        p_txDistribution_ = p_txDistribution;
        numRounds_ = numRounds;
    }

    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
        followees_ = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        pendingTransactions_ = new HashSet<>(pendingTransactions);
    }

    public Set<Transaction> getProposals() {
        // IMPLEMENT THIS

        // since after final round, behavior of getProposals changes, we need to clear
        // the old set of transctions and create a temp set to hold the old values

        HashSet<Transaction> re = new HashSet<Transaction>(pendingTransactions_);
        pendingTransactions_.clear();
        return re;
    }

    public void receiveCandidates(ArrayList<Integer[]> candidates) {
        // IMPLEMENT THIS
        for (int i = 0; i < followees_.length; i++) {
            if (followees_[i] && !candidates.contains(i)) {
                malicious_nodes[i] = true;
            }
        }

        for (Integer candidate: candidates) {
            if (!malicious_nodes[candidate.])
        }
    }
}
