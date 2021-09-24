import sys
import dotenv
from utils.authenticator import Authenticator

DOTENV_PASSWORD_KEY = "PASSWORD"


def main():
    if len(sys.argv) < 3:
        print(
            f"USAGE: python3 {sys.argv[0]} RAW_PASSWORD DOTENV_FILE_PATH", file=sys.stderr)
        return

    password = Authenticator.hash_password(
        sys.argv[1]).decode(encoding='ascii')

    dotenv.set_key(sys.argv[2], DOTENV_PASSWORD_KEY, password)
    print(f'Saved using key: "{DOTENV_PASSWORD_KEY}"')


if __name__ == '__main__':
    main()
