package com.ergosapiens.ergopay;

import org.ergoplatform.P2PKAddress;
import org.ergoplatform.appkit.*;
import org.ergoplatform.appkit.impl.ErgoTreeContract;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import special.sigma.SigmaProp;
import spire.math.Algebraic;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.ergoplatform.appkit.Parameters.MinFee;

@RestController
public class ErgoPayController {

    private ReducedTransaction getReducedTx(boolean isMainNet, long amountToSpend, List<ErgoToken> tokensToSpend,
                                            Address sender,
                                            Function<UnsignedTransactionBuilder, UnsignedTransactionBuilder> outputBuilder) {
        NetworkType networkType = isMainNet ? NetworkType.MAINNET : NetworkType.TESTNET;
        return RestApiErgoClient.create(
                getDefaultNodeUrl(isMainNet),
                networkType,
                "",
                RestApiErgoClient.getDefaultExplorerUrl(networkType)
        ).execute(ctx -> {

            List<InputBox> boxesToSpend = BoxOperations.createForSender(sender, ctx)
                    .withAmountToSpend(amountToSpend)
                    .withTokensToSpend(tokensToSpend)
                    .loadTop();

            P2PKAddress changeAddress = sender.asP2PK();
            UnsignedTransactionBuilder txB = ctx.newTxBuilder();

            UnsignedTransactionBuilder unsignedTransactionBuilder = txB.boxesToSpend(boxesToSpend)
                    .fee(MinFee)
                    .sendChangeTo(changeAddress);

            UnsignedTransaction unsignedTransaction = outputBuilder.apply(unsignedTransactionBuilder).build();

            return ctx.newProverBuilder().build().reduce(unsignedTransaction, 0);
        });
    }

    @GetMapping("/ergopay/{proxyAddress}/{buyerAddress}/{amountERG}/{amountComet}")
    public ErgoPayResponse mintToken(@PathVariable String buyerAddress, @PathVariable String proxyAddress, @PathVariable double amountERG, @PathVariable long amountComet) {

        ErgoPayResponse response = new ErgoPayResponse();
        long nanoERGs = (long)Math.pow(10, 9);


        try {
            boolean isMainNet = isMainNetAddress(buyerAddress);
            long amountToSend = (long)(amountERG * nanoERGs);
            Address buyer = Address.create(buyerAddress);
            Address proxy = Address.create(proxyAddress);
            ErgoToken comet = new ErgoToken(COMET_TOKEN_ID, amountComet);
            ErgoValue<SigmaProp> sigmaProp = ErgoValue.of(buyer.getPublicKey());


            byte[] reduced = getReducedTx(isMainNet, amountToSend, Collections.emptyList(), buyer,
                    unsignedTxBuilder -> {

                        NetworkType networkType = isMainNet ? NetworkType.MAINNET : NetworkType.TESTNET;

                        OutBoxBuilder outBoxBuilder = unsignedTxBuilder.outBoxBuilder()
                                .value(amountToSend)
                                .tokens(comet)
                                .contract(proxy.toErgoContract())
                                .registers(sigmaProp);

                        OutBox newBox = outBoxBuilder.build();

                        unsignedTxBuilder.outputs(newBox);

                        return unsignedTxBuilder;
                    }
            ).toBytes();

            response.reducedTx = Base64.getUrlEncoder().encodeToString(reduced);
            response.address = buyerAddress;
            response.message = "Transaction built, ensure that no url parameters have been changed";
            response.messageSeverity = ErgoPayResponse.Severity.INFORMATION;

        } catch (Throwable t) {
            response.messageSeverity = ErgoPayResponse.Severity.ERROR;
            response.message = (t.getMessage());
        }

        return response;
    }

    @GetMapping("/ergopay/{proxyAddress}/{buyerAddress}/{sendToAddress}/{amountERG}/{amountComet}")
    public ErgoPayResponse mintToken(@PathVariable String buyerAddress, @PathVariable String sendToAddress, @PathVariable String proxyAddress, @PathVariable double amountERG, @PathVariable long amountComet) {

        ErgoPayResponse response = new ErgoPayResponse();
        long nanoERGs = (long)Math.pow(10, 9);


        try {
            boolean isMainNet = isMainNetAddress(buyerAddress);
            long amountToSend = (long)(amountERG * nanoERGs);
            Address buyer = Address.create(buyerAddress);
            Address proxy = Address.create(proxyAddress);
            Address sendTo = Address.create(sendToAddress);
            ErgoToken comet = new ErgoToken(COMET_TOKEN_ID, amountComet);
            ErgoValue<SigmaProp> sigmaProp = ErgoValue.of(sendTo.getPublicKey());


            byte[] reduced = getReducedTx(isMainNet, amountToSend, Collections.emptyList(), buyer,
                    unsignedTxBuilder -> {

                        NetworkType networkType = isMainNet ? NetworkType.MAINNET : NetworkType.TESTNET;

                        OutBoxBuilder outBoxBuilder = unsignedTxBuilder.outBoxBuilder()
                                .value(amountToSend)
                                .tokens(comet)
                                .contract(proxy.toErgoContract())
                                .registers(sigmaProp);

                        OutBox newBox = outBoxBuilder.build();

                        unsignedTxBuilder.outputs(newBox);

                        return unsignedTxBuilder;
                    }
            ).toBytes();

            response.reducedTx = Base64.getUrlEncoder().encodeToString(reduced);
            response.address = buyerAddress;
            response.message = "Transaction built, ensure that no url parameters have been changed. " + sendToAddress + " will get the ticket";
            response.messageSeverity = ErgoPayResponse.Severity.INFORMATION;

        } catch (Throwable t) {
            response.messageSeverity = ErgoPayResponse.Severity.ERROR;
            response.message = (t.getMessage());
        }

        return response;
    }

    private static boolean isMainNetAddress(String address) {
        try {
            return Address.create(address).isMainnet();
        } catch (Throwable t) {
            throw new IllegalArgumentException("Invalid address: " + address);
        }
    }
    private static String getDefaultNodeUrl(boolean mainNet) {
        return mainNet ? NODE_MAINNET : NODE_TESTNET;
    }

    public static final String NODE_MAINNET = "http://213.239.193.208:9053/";
    public static final String NODE_TESTNET = "http://213.239.193.208:9052/";
    private final String COMET_TOKEN_ID = "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b";
}
