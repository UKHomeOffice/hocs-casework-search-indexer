FROM quay.io/ukhomeofficedigital/hocs-base-image:4.1.5 as builder

WORKDIR /builder

COPY ./build/libs/hocs-*.jar .

RUN java -Djarmode=layertools -jar hocs-*.jar extract

FROM quay.io/ukhomeofficedigital/hocs-base-image:4.1.5

WORKDIR /app

COPY --chown=user_hocs:group_hocs ./scripts/run.sh ./
COPY --from=builder --chown=user_hocs:group_hocs ./builder/spring-boot-loader/ ./
COPY --from=builder --chown=user_hocs:group_hocs ./builder/dependencies/ ./
COPY --from=builder --chown=user_hocs:group_hocs ./builder/application/ ./

USER 10000

CMD ["sh", "/app/run.sh"]
