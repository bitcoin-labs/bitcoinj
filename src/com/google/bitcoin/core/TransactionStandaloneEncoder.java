
package com.google.bitcoin.core;

import java.math.BigInteger;
import java.util.*;

import static com.google.bitcoin.core.Utils.*;


public class TransactionStandaloneEncoder {

    NetworkParameters params;

    ArrayList<ECKey> in_keys;
    ArrayList<byte[]> in_hashes;
    ArrayList<Integer> in_indexes;

    ArrayList<Address> out_addresses;
    ArrayList<BigInteger> out_amounts;

    public TransactionStandaloneEncoder(NetworkParameters params) {
        this.params = params;
        this.in_keys = new ArrayList<ECKey>();
        this.in_hashes = new ArrayList<byte[]>();
        this.in_indexes = new ArrayList<Integer>();
        this.out_addresses = new ArrayList<Address>();
        this.out_amounts = new ArrayList<BigInteger>();
    }

    public void addInput(ECKey key, int txIndex, String txHashHex) {
        addInput(key, txIndex, Utils.hexStringToBytes(txHashHex));
    }

    public void addInput(ECKey key, int txIndex, byte[] txHash) {
        in_keys.add(key);
        in_hashes.add(txHash);
        in_indexes.add(txIndex);
    }

    public void addOutput(BigInteger amount, String address) throws AddressFormatException {
        NetworkParameters params = NetworkParameters.prodNet();
        out_addresses.add(new Address(params, address));
        out_amounts.add(amount);
    }

    public Transaction createSignedTransaction() {

        NetworkParameters params = NetworkParameters.prodNet();
        Wallet wallet = new Wallet(params);
        for (int i = 0; i < in_keys.size(); i++) {
            wallet.addKey(in_keys.get(i));
        }

        Transaction sendTx = new Transaction(params);

        // Outputs
        for (int i = 0; i < out_amounts.size(); i++) {
            sendTx.addOutput(new TransactionOutput(params, out_amounts.get(i), out_addresses.get(i), sendTx));
        }

        // Inputs
        Transaction t_in;
        Address whatever = new ECKey().toAddress(params);
        for (int i = 0; i < in_keys.size(); i++) {

            t_in = new Transaction(params);

            // index (via spacers)
            int index = in_indexes.get(i).intValue();
            for (int j = 0; j < index; j++) {
                t_in.addOutput(new TransactionOutput(params, Utils.toNanoCoins(1, 0), whatever, t_in));
            }

            // hash
            t_in.setFakeHashForTesting(new Sha256Hash(in_hashes.get(i)));

            try {
                wallet.receive(t_in, null, BlockChain.NewBlockType.BEST_CHAIN);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            sendTx.signInputs(Transaction.SigHash.ALL, wallet);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        return sendTx;
    }
}

