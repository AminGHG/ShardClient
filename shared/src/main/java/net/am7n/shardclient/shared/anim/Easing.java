package net.am7n.shardclient.shared.anim;

@FunctionalInterface
public interface Easing {
    float apply(float t);

    Easing LINEAR = t -> t;

    Easing IN_CUBIC = t -> t * t * t;

    Easing OUT_CUBIC = t -> {
        float u = 1 - t;
        return 1 - u * u * u;
    };

    Easing IN_OUT_CUBIC = t -> {
        if (t < 0.5f) {
            return 4 * t * t * t;
        }
        float u = -2 * t + 2;
        return 1 - u * u * u / 2;
    };

    Easing OUT_QUART = t -> {
        float u = 1 - t;
        return 1 - u * u * u * u;
    };

    Easing OUT_EXPO = t -> t >= 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);

    Easing OUT_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        float u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    };

    Easing IN_OUT_SINE = t -> (float) (-(Math.cos(Math.PI * t) - 1) / 2);
}
