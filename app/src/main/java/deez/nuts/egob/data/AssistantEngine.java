package deez.nuts.egob.data;

/**
 * Minimal local assistant. Every question receives the same short answer so the demo behaves
 * predictably without a backend or external AI service.
 */
public final class AssistantEngine {

    public static final String SINGLE_REPLY =
            "Lo siento, todavía no puedo ayudarte con eso.";

    public static final class Reply {
        public final String text;
        public final int serviceId;

        Reply(String text, int serviceId) {
            this.text = text;
            this.serviceId = serviceId;
        }
    }

    public Reply reply(String rawInput) {
        return new Reply(SINGLE_REPLY, 0);
    }
}
