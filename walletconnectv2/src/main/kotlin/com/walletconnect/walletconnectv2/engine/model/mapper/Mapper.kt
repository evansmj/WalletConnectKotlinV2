package com.walletconnect.walletconnectv2.engine.model.mapper

import com.walletconnect.walletconnectv2.common.model.Expiry
import com.walletconnect.walletconnectv2.common.model.Topic
import com.walletconnect.walletconnectv2.common.model.Ttl
import com.walletconnect.walletconnectv2.crypto.model.PublicKey
import com.walletconnect.walletconnectv2.engine.model.EngineData
import com.walletconnect.walletconnectv2.relay.model.JsonRpcMethod
import com.walletconnect.walletconnectv2.relay.model.clientsync.pairing.Pairing
import com.walletconnect.walletconnectv2.relay.model.clientsync.pairing.before.PreSettlementPairing
import com.walletconnect.walletconnectv2.relay.model.clientsync.pairing.before.proposal.*
import com.walletconnect.walletconnectv2.relay.model.clientsync.pairing.before.success.PairingParticipant
import com.walletconnect.walletconnectv2.relay.model.clientsync.pairing.before.success.PairingState
import com.walletconnect.walletconnectv2.relay.model.clientsync.session.Session
import org.json.JSONObject
import java.net.URI
import kotlin.time.Duration

internal fun String.toPairProposal(): Pairing.Proposal {
    val properUriString = if (contains("wc://")) this else replace("wc:", "wc://")
    val pairUri = URI(properUriString)
    val mapOfQueryParameters: Map<String, String> =
        pairUri.query.split("&")
            .associate { query -> query.substringBefore("=") to query.substringAfter("=") }
    val relay = JSONObject(mapOfQueryParameters["relay"] ?: "{}")
    val publicKey = mapOfQueryParameters["publicKey"] ?: ""
    val controller: Boolean = mapOfQueryParameters["controller"].toBoolean()
    val ttl: Long = Duration.days(30).inWholeSeconds

    return Pairing.Proposal(
        topic = Topic(pairUri.userInfo),
        relay = relay,
        pairingProposer = PairingProposer(publicKey, controller),
        pairingSignal = PairingSignal("uri", PairingSignalParams(properUriString)),
        permissions = PairingProposedPermissions(JsonRPC(listOf(JsonRpcMethod.WC_SESSION_PROPOSE))),
        ttl = Ttl(ttl)
    )
}

internal fun Pairing.Proposal.toPairingSuccess(settleTopic: Topic, expiry: Expiry, selfPublicKey: PublicKey): Pairing.Success =
    Pairing.Success(
        settledTopic = settleTopic,
        relay = relay,
        responder = PairingParticipant(publicKey = selfPublicKey.keyAsHex),
        expiry = expiry,
        state = PairingState(null)
    )

internal fun Pairing.Proposal.toApprove(
    id: Long,
    settleTopic: Topic,
    expiry: Expiry,
    selfPublicKey: PublicKey
): PreSettlementPairing.Approve = PreSettlementPairing.Approve(id = id, params = this.toPairingSuccess(settleTopic, expiry, selfPublicKey))

internal fun Session.Proposal.toSessionProposal(): EngineData.SessionProposalDO =
    EngineData.SessionProposalDO(
        name = this.proposer.metadata?.name!!,
        description = this.proposer.metadata.description,
        url = this.proposer.metadata.url,
        icons = this.proposer.metadata.icons.map { URI(it) },
        chains = this.permissions.blockchain.chains,
        methods = this.permissions.jsonRpc.methods,
        types = this.permissions.notifications.types,
        topic = this.topic.value,
        proposerPublicKey = this.proposer.publicKey,
        ttl = this.ttl.seconds,
        accounts = listOf()
    )