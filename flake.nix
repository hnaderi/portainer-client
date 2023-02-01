{
  inputs = {
    typelevel-nix.url = "github:typelevel/typelevel-nix";
    nixpkgs.follows = "typelevel-nix/nixpkgs";
    flake-utils.follows = "typelevel-nix/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, typelevel-nix }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ typelevel-nix.overlay ];
        };
        curl = (pkgs.curlFull.override { gssSupport = false; }).overrideAttrs
          (old: { dontDisableStatic = true; });

      in {
        devShell = pkgs.devshell.mkShell {
          imports = [ typelevel-nix.typelevelShell ];
          name = "portainer-cli";
          typelevelShell = {
            jdk.package = pkgs.jdk17;
            nodejs.enable = true;
            native = {
              enable = true;
              libraries = [
                curl
                pkgs.upx
              ];
            };
          };
        };
      });
}
