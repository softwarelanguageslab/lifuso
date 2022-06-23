public static void main(final String[] args) throws Exception {
final ImmutableList.Builder<Integer, String> lb = ImmutableList.builder();
    for (final String arg : args) {
        lb.add(Integer.parseInt(arg));
    }
}