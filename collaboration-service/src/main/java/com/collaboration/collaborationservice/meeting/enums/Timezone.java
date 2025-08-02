package com.collaboration.collaborationservice.meeting.enums;

    public enum Timezone {
        EUROPE_PARIS("+01:00"),
        UTC("+00:00"),
        AMERICA_NEW_YORK("-04:00");

        private final String offset;

        Timezone(String offset) {
            this.offset = offset;
        }

        public String getOffset() {
            return offset;
        }
}
